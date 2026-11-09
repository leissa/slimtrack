#!/usr/bin/env bash
set -euo pipefail

usage() {
    cat <<'EOF'
Usage: tools/release.sh [VERSION] [--skip-checks] [-y|--yes]

Bumps versionCode, sets versionName to VERSION (default: next minor), commits,
tags vVERSION and pushes the branch and tag. Pushing the tag starts the Release
workflow, which builds, signs and publishes the APK to the F-Droid repository.

Examples:
  tools/release.sh 1.1            # release 1.1
  tools/release.sh 1.1 --skip-checks
  tools/release.sh                # release 1.1 if the current version is 1.0

Options:
  --skip-checks   Do not run unit tests, lint and the release build first
  -y, --yes       Do not ask for confirmation
  -h, --help      Show this help
EOF
}

fail() {
    echo "error: $*" >&2
    exit 1
}

skip_checks=0
assume_yes=0
version=""

for arg in "$@"; do
    case "$arg" in
        -h|--help) usage; exit 0 ;;
        --skip-checks) skip_checks=1 ;;
        -y|--yes) assume_yes=1 ;;
        -*) fail "unknown option: $arg" ;;
        *)
            [ -z "$version" ] || fail "only one version may be given"
            version="$arg"
            ;;
    esac
done

cd "$(git rev-parse --show-toplevel)"

gradle_file=app/build.gradle.kts
[ -f "$gradle_file" ] || fail "$gradle_file not found; run this from the SlimTrack repository"

current_code=$(sed -n 's/^[[:space:]]*versionCode = \([0-9][0-9]*\).*/\1/p' "$gradle_file")
current_name=$(sed -n 's/^[[:space:]]*versionName = "\([^"]*\)".*/\1/p' "$gradle_file")
[ -n "$current_code" ] || fail "could not read versionCode from $gradle_file"
[ -n "$current_name" ] || fail "could not read versionName from $gradle_file"

if [ -n "$(git status --porcelain --untracked-files=no)" ]; then
    fail "working tree has uncommitted changes; commit or stash them first"
fi

branch=$(git symbolic-ref --short HEAD 2>/dev/null) || fail "detached HEAD; check out a branch first"

if [ -z "$version" ]; then
    IFS=. read -r -a parts <<< "$current_name"
    last_index=$((${#parts[@]} - 1))
    last=${parts[$last_index]}
    [[ $last =~ ^[0-9]+$ ]] || fail "cannot guess the next version from '$current_name'; pass one explicitly"
    parts[$last_index]=$((last + 1))
    version=$(IFS=.; echo "${parts[*]}")
fi

[[ $version =~ ^[0-9A-Za-z][0-9A-Za-z._-]*$ ]] || fail "invalid version name: $version"
[ "$version" != "$current_name" ] || fail "version is already $current_name; choose a new one"

new_code=$((current_code + 1))
tag="v$version"

if git rev-parse -q --verify "refs/tags/$tag" >/dev/null; then
    fail "tag $tag already exists"
fi

sed -i -E "s/^([[:space:]]*versionCode = )[0-9]+/\1$new_code/" "$gradle_file"
sed -i -E "s/^([[:space:]]*versionName = )\"[^\"]*\"/\1\"$version\"/" "$gradle_file"

echo "Preparing SlimTrack $version (versionCode $new_code) on branch $branch"
git --no-pager diff -- "$gradle_file"

if [ "$assume_yes" -eq 0 ] && [ -t 0 ]; then
    read -r -p "Publish $version? [y/N] " answer
    if [[ ! $answer =~ ^[Yy]$ ]]; then
        git checkout -- "$gradle_file"
        echo "Aborted."
        exit 1
    fi
fi

if [ "$skip_checks" -eq 0 ]; then
    echo "Running unit tests, lint and release build (use --skip-checks to skip)..."
    if ! ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease; then
        git checkout -- "$gradle_file"
        fail "checks failed; the version bump was reverted"
    fi
fi

git add "$gradle_file"
git commit -m "Release $version"
git tag -a "$tag" -m "SlimTrack $version"

git push origin "$branch"
git push origin "refs/tags/$tag"

cat <<EOF

Published $tag to origin. The Release workflow now builds and signs the APK and
updates the F-Droid repository. Follow it with:

    gh run list --workflow Release --limit 1
    gh run watch --workflow Release
EOF
