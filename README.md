# SlimTrack

A small diet and weight diary for Android. Log meals per meal, track calories and macros
against daily goals, record weight and exercise, and see trends over time.

Everything is stored in a local Room database. Only food search and barcode lookup go
online, and they send nothing but the search term or the scanned barcode. Android Auto
Backup and device-to-device transfer are disabled, so the diary never leaves the device
(see `backup_rules.xml` / `data_extraction_rules.xml`).

## Features

- Daily diary with four meals, calories and macros, exercise that raises the day's budget
- Weight entries with BMI, target weight/BMI and a chart showing every point plus 3-day and
  7-day averages, in metric or imperial units
- Diet tiers (fast / medium / slow) and a goal recommendation based on BMR and activity
- Food search across three sources:
  - **BLS 4.0** — 7,140 German foods, bundled offline
  - **FDDB** — German branded and user-entered products
  - **Open Food Facts** — international branded products and barcode lookup
- Barcode scanning with CameraX and ML Kit

## Stack

- Kotlin 2.2 + Jetpack Compose (Material 3)
- Room 2.8 (KSP) for persistence
- Lifecycle ViewModel + StateFlow, manual dependency container
- CameraX 1.6 + ML Kit barcode scanning
- AGP 9.5 / Gradle 9.6 · minSdk 26 · targetSdk 37

## Build and run

```bash
./gradlew :app:assembleDebug    # build the APK
./gradlew :app:installDebug     # install on the connected device/emulator
adb shell am start -n com.leissa.slimtrack/.MainActivity
```

Or open the project folder in Android Studio and press Run.

Start the emulator without Android Studio:

```bash
~/Android/Sdk/emulator/emulator -avd <your-avd>
```

## Tests, lint and continuous integration

```bash
./gradlew :app:testDebugUnitTest   # JVM unit tests
./gradlew :app:lintDebug           # Android lint
./gradlew :app:assembleDebug       # debug APK
./gradlew :app:assembleRelease     # release APK (unsigned without a signing config)
```

`.github/workflows/ci.yml` runs unit tests, lint, and the debug and release builds on
every push to the default branch, every pull request, and on manual dispatch. Failed runs
upload the test and lint reports; successful runs keep the debug APK as a workflow
artifact for 14 days. No signing secrets are needed for CI.

## Release signing

Create a keystore once and keep it outside the repository:

```bash
mkdir -p ~/keystores
keytool -genkeypair -v \
    -keystore ~/keystores/slimtrack-release.jks \
    -alias slimtrack \
    -keyalg RSA -keysize 4096 -validity 10000 \
    -dname "CN=SlimTrack"
```

`keytool` asks for the store and key passwords. Back up the keystore and both passwords:
Android only installs an update when it is signed with the same key, so losing them means
existing users have to uninstall and reinstall.

For local builds, create `keystore.properties` in the repository root (it is ignored by
git). Use an absolute path, or a path relative to the repository root:

```properties
storeFile=/home/you/keystores/slimtrack-release.jks
storePassword=change-me
keyAlias=slimtrack
keyPassword=change-me
```

CI instead passes the same values through environment variables:

| Variable | Meaning |
| --- | --- |
| `SLIMTRACK_KEYSTORE` | Path to the keystore file |
| `SLIMTRACK_KEYSTORE_PASSWORD` | Keystore password |
| `SLIMTRACK_KEY_ALIAS` | Key alias |
| `SLIMTRACK_KEY_PASSWORD` | Key password |

When neither source is complete, `:app:assembleRelease` still builds, but the APK is left
unsigned. That keeps CI and fresh checkouts working without secrets.

Before every release, increase `versionCode` and set a new `versionName` in
`app/build.gradle.kts`. F-Droid only offers an update when `versionCode` is higher than
the version it already knows.

## Publishing a release and the F-Droid repository

`.github/workflows/release.yml` runs the unit tests and lint, builds a signed APK, verifies
its signature, generates an F-Droid repository index with `fdroidserver`, and publishes
the repository to GitHub Pages. It runs when a `v*` tag is pushed or when started manually
from the Actions tab.

The workflow installs a pinned `fdroidserver` release from PyPI into a temporary virtual
environment. Ubuntu's packaged `fdroidserver` is older and its APK parser cannot read
resources from APKs built with recent Android Gradle Plugin versions. If you validate the
repository locally, install the same version:

```bash
python3 -m venv .fdroid-venv
.fdroid-venv/bin/pip install fdroidserver==2.4.5
```

A typical release:

```bash
# bump versionCode and versionName in app/build.gradle.kts first
git add app/build.gradle.kts
git commit -m "Release 1.1"
git tag v1.1
git push origin master v1.1
```

The workflow then builds and signs the APK, updates the repository, and attaches the APK
to the GitHub release for the tag.

The repository is served at:

```text
https://<owner>.github.io/<repository>/fdroid/repo/
```

for example `https://example.github.io/slimtrack/fdroid/repo/`. If the repository is named
`<owner>.github.io`, leave out the `/<repository>` part; the workflow detects that case.

One-time GitHub setup:

1. Push this project to GitHub.
2. Add the secrets below under **Settings → Secrets and variables → Actions**. Generate
   the base64 values with `base64 -w0 <file>` on Linux or
   `base64 -i <file> | tr -d '\n'` on macOS.
3. Under **Settings → Pages**, set the source to **GitHub Actions**.
4. Run the **Release** workflow once, either by pushing a tag such as `v1.0` or with
   **Run workflow**.

| Secret | Value |
| --- | --- |
| `APP_KEYSTORE_BASE64` | App signing keystore, base64 encoded |
| `APP_KEYSTORE_PASSWORD` | App keystore password |
| `APP_KEY_ALIAS` | App key alias |
| `APP_KEY_PASSWORD` | App key password |
| `FDROID_KEYSTORE_BASE64` | Repository signing keystore, base64 encoded |
| `FDROID_KEYSTORE_PASSWORD` | Repository keystore password |
| `FDROID_KEY_PASSWORD` | Repository key password |

Encode the two keystores with:

```bash
base64 -w0 ~/keystores/slimtrack-release.jks   # APP_KEYSTORE_BASE64
base64 -w0 ~/keystores/slimtrack-fdroid.jks    # FDROID_KEYSTORE_BASE64
```

`APP_KEY_ALIAS` is the alias you chose when creating the app key (`slimtrack` in the
example above).

The app keystore signs APKs. The repository keystore signs the F-Droid index and must
stay stable across releases. Create it once, with the alias `slimtrack`:

```bash
keytool -genkeypair -v \
    -keystore ~/keystores/slimtrack-fdroid.jks \
    -alias slimtrack \
    -keyalg RSA -keysize 4096 -validity 10000 \
    -dname "CN=SlimTrack F-Droid Repository"
```

Put its base64 encoding in `FDROID_KEYSTORE_BASE64`. Back it up as well; a new repository
key makes every client reject the new index until the repository is removed and added
again.

The workflow publishes only the newest APK. The app's F-Droid metadata lives in
`fdroid/metadata/com.leissa.slimtrack.yml`; it declares `NonFreeNet` because food search
uses FDDB, and the description notes that barcode scanning uses Google ML Kit.

### Installing from the repository

On the phone, open **F-Droid → Settings → Repositories → +**, paste the repository URL,
and add it. Then refresh the repository list and install or update SlimTrack from F-Droid.
For tag builds, the same APK is also attached to the GitHub release for manual
installation; manual runs keep it as a workflow artifact.

The app is not submitted to f-droid.org:

- ML Kit barcode scanning is proprietary, and FDDB is a non-free network service.
- Official builds would need a reproducible buildserver with AGP 9.5 and SDK 37 support.

A self-hosted binary repository is therefore the practical option today.

## Project layout

```
app/src/main/java/com/leissa/slimtrack/
├── SlimTrackApp.kt          Application, owns AppContainer
├── MainActivity.kt
├── data/                    Room entities, DAOs, database, seed foods, BLS table
│   └── remote/              FDDB and Open Food Facts clients
├── ui/
│   ├── SlimTrackRoot.kt     bottom navigation
│   ├── today/               daily diary, add-food sheet, exercise sheet
│   ├── history/             per-day totals and net calories
│   ├── weight/              weight entries, chart and targets
│   ├── scan/                barcode scanner dialog
│   ├── settings/            profile, goals and diet tiers
│   └── components/          shared progress bars
└── util/                    units, dates and number formatting
tools/extract_bls.py         regenerates the bundled BLS table from the official workbook
.github/workflows/           CI and release automation
fdroid/metadata/             F-Droid app metadata
```

Room schema exports land in `app/schemas/` — keep them in version control so future
schema changes can be migrated safely.

## Data model

- `Food` — name, serving label ("100 g", "1 medium"), calories and macros per serving.
- `DiaryEntry` — date (epoch day), meal, food reference, number of servings.
- `Goals` — single row with daily calorie/protein/carb/fat targets.
- `Profile` — units, sex, age, height, activity level, diet tier and target weight.
- `WeightEntry` — one per day (epoch day, kg).
- `ExerciseEntry` — date, activity, minutes and the calories burned at logging time.

The first launch seeds a starter food list (`data/SeedFoods.kt`).

## Food data

`app/src/main/assets/bls_foods.tsv` contains name, kcal, protein, fat and carbs per 100 g
for 7,140 generic foods from the **Bundeslebensmittelschlüssel 4.0**. Zero-calorie foods
such as water and unsweetened tea are included. It is generated with:

```bash
python3 tools/extract_bls.py \
    /path/to/BLS_4_0_2025_DE/BLS_4_0_Daten_2025_DE.xlsx \
    app/src/main/assets/bls_foods.tsv
```

The official workbook is a free download from <https://blsdb.de> (CC BY 4.0).

FDDB has no public API. `FddbClient` reads the anonymous website search page and fetches
nutrition from a product page only when the user picks that result. Keep requests sparse.

Attribution shown in the app's Profile screen:

- Bundeslebensmittelschlüssel 4.0, © Max Rubner-Institut (2025), CC BY 4.0,
  DOI 10.25826/Data20251217-134202-0
- Open Food Facts, ODbL
- FDDB, <https://fddb.info>

## License

SlimTrack is released under the MIT License; see [LICENSE](LICENSE). The bundled
Bundeslebensmittelschlüssel data keeps its own CC BY 4.0 license and attribution.

## Ideas for later

- Edit the servings of an existing entry
- Copy a whole day's meals
- CSV export
- Search synonyms (e.g. "yogurt" → "Joghurt" in the BLS table)
