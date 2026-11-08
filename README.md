# SlimTrack

A small diet and weight diary for Android. Log meals per meal, track calories and macros
against daily goals, record weight and exercise, and see trends over time.

Everything is stored in a local Room database. Only food searches go online, and they send
nothing but the search term.

## Features

- Daily diary with four meals, calories and macros, exercise that raises the day's budget
- Weight entries with BMI, target weight/BMI and a chart showing every point plus 3-day and
  7-day averages, in metric or imperial units
- Diet tiers (fast / medium / slow) and a goal recommendation based on BMR and activity
- Food search across three sources:
  - **BLS 4.0** — 7,100 German foods, bundled offline
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
~/Android/Sdk/emulator/emulator -avd Medium_Phone_API_37.0
```

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
for 7,106 generic foods from the **Bundeslebensmittelschlüssel 4.0**. It is generated with:

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

## Ideas for later

- Edit the servings of an existing entry
- Copy a whole day's meals
- CSV export
- Search synonyms (e.g. "yogurt" → "Joghurt" in the BLS table)
