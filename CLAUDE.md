# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This App Does

CbrfApp displays daily foreign exchange rates from the Central Bank of Russia (CBRF). It fetches public XML feeds (no API key), caches rates in Room, and shows them in a Compose UI and three Glance home screen widgets (Small 1×1, Medium 1×2, Large 2×2).

## Build Commands

```bash
./gradlew assembleDebug          # Debug build
./gradlew assembleRelease        # Release build (requires signing config)
./gradlew installDebug           # Build and install to connected device
./gradlew test                   # Unit tests
./gradlew connectedAndroidTest   # Instrumented tests (device/emulator)
./gradlew lint                   # Lint checks
./gradlew ktfmtFormat            # Format Kotlin code
```

All dependencies are declared in `gradle/libs.versions.toml` and accessed via `libs.*` aliases.

Min SDK: 31, Target SDK: 35, JDK: 17. AGP 9.2 / Gradle 9.6 / Kotlin 2.2 (requires Android Studio Panda 4+).

### AGP 9 Build Constraints

- **Built-in Kotlin**: AGP 9 compiles Kotlin itself — the `org.jetbrains.kotlin.android` plugin is intentionally absent (applying it fails with "Cannot add extension with name 'kotlin'"). There is no `kotlinOptions` block; `jvmTarget` comes from `compileOptions.targetCompatibility`.
- **KSP** must stay on the Kotlin-decoupled `2.3+` line; `2.2.x-y.z` versions are incompatible with built-in Kotlin.
- **`compileOnly(libs.errorprone.annotations)`** in `app/build.gradle.kts` is required: Hilt 2.60's generated code references `@CanIgnoreReturnValue` without pulling the annotations artifact onto the compile classpath. Do not remove it as "unused".

## Architecture

Three-layer clean architecture:

```
presentation/  → Compose screens, ViewModels, Glance widgets
domain/        → Use cases, repository interface, domain models
data/          → Room DB, Retrofit + XML parsers, DataStore, repository impl
```

**Data flow**: `CbrfApi` (Retrofit, raw XML) → `CbrfXmlParser` / `CurrencyValParser` (Windows-1251 pull parsers) → `RateRepositoryImpl` (caching + date logic) → use cases → ViewModels (StateFlow) → Compose UI + Glance widgets.

**DI**: Hilt throughout. Widgets use `WidgetEntryPoint` (Hilt entry point, not `@AndroidEntryPoint`).

**Background updates**: Single `RateUpdateWorker` (WorkManager + Hilt) with configurable interval (1h/3h/6h/12h/24h). Rescheduled on settings changes. Network connectivity required.

## Key Patterns to Know

### Date Fallback
When today's rates aren't published (weekends/holidays), CBR responds to `date_req=today` with the latest published rates and their date; `RateRepositoryImpl` reads it from the `ValCurs Date` attribute and backfills the cache up to yesterday, leaving today's slot empty. `RefreshTodayRatesUseCase` then resolves the effective date via `getLatestAvailableDate` — no day-by-day walk-back. Tomorrow's rates are cached opportunistically. Data older than 60 days is auto-evicted.

### Widget State (Glance)
Widgets use `PreferencesGlanceStateDefinition`. `WidgetUpdateHelper` calls `loadData()` → `updateAppWidgetState()` → `widget.update()`. Inside `provideGlance()`, call `loadDataAndPersistState()` once, then read `currentState<Preferences>()` reactively — never capture static data in `provideContent{}`. `DateChangedReceiver` triggers a refresh at midnight.

### Widget Layout
`BaseRateWidget` uses `SizeMode.Exact` so `LocalSize.current` returns actual size on resize. All widgets: `minResizeWidth=54dp`, `minResizeHeight=54dp`.

### Settings & Localization
- **Theme** (system/light/dark): DataStore, read as Flow
- **Language** (system/EN/RU): SharedPreferences (read synchronously in `attachBaseContext` via `LocaleHelper`), also mirrored to DataStore. Language changes require Activity recreation.
- **Widget preferences**: DataStore, per-widget currency selection via `WidgetConfigActivity`

### XML Parsing
CBRF returns Windows-1251 encoded XML. Two custom pull parsers handle this: `CbrfXmlParser` (daily rates) and `CurrencyValParser` (currency metadata). Retrofit uses `ScalarsConverterFactory` to get raw `ResponseBody`.

### Trend Colors
`GetRatesForDisplayUseCase` computes up/down trends by comparing to the previous available date's rates. Green = up, Red = down (invertible in settings for color-blind users). Tomorrow's rate color is relative to today.

## API Endpoints

| Endpoint | Description |
|---|---|
| `GET https://www.cbr.ru/scripts/XML_daily.asp?date_req=DD/MM/YYYY` | Daily rates |
| `GET https://www.cbr.ru/scripts/XML_val.asp` | Currency metadata |

## Database

Room database `cbrf_rates.db`, version 3, `fallbackToDestructiveMigration` enabled. To change schema: update the entity, increment `RateDatabase.version`.
