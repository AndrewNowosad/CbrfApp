# CBRF Rates

Android app that shows daily foreign exchange rates from the [Central Bank of the Russian Federation](https://www.cbr.ru/).

<!-- Screenshots: add images here once available -->

## Features

**Main screen**
- Full list of CBRF currency rates for today (and tomorrow when published)
- Color-coded trend indicators — green/red (invertible for color-blind users)
- Date picker for historical rate lookup
- Pull-to-refresh

**Home screen widgets** (Jetpack Glance)
| Widget | Grid | Currencies shown |
|--------|------|-----------------|
| Small  | 1×1  | 1               |
| Medium | 1×2  | up to 2         |
| Large  | 2×2  | up to 4         |

Each widget is individually configurable (currency selection, ordering) and auto-refreshes in the background.

**Settings**
- Update interval: 1 h / 3 h / 6 h / 12 h / 24 h
- Decimal places: 2 or 4
- Invert trend colors
- Language: system / English / Russian
- Theme: system / light / dark
- Widget background mode and corner radius

## Tech stack

| Layer | Libraries |
|-------|-----------|
| UI | Jetpack Compose (BOM 2024.09), Glance 1.1, Material 3 |
| DI | Hilt 2.51 |
| Data | Room 2.6, DataStore 1.1, Retrofit 2.11 + OkHttp 4.12, kotlinx.serialization |
| Background | WorkManager 2.9 |
| Language | Kotlin 2.0, Coroutines 1.8 |

## Architecture

Three-layer clean architecture:

```
presentation/   — Compose screens, ViewModels, Glance widgets
domain/         — use cases, repository interfaces, UI models
data/           — Room DB, Retrofit API, DataStore preferences, XML parsers
```

Dependency injection via Hilt with entry points for widgets and workers.

## Build

**Requirements:** Android Studio Hedgehog+, JDK 17, min SDK 31, target SDK 35.

```bash
# debug
./gradlew assembleDebug

# release (requires signing config)
./gradlew assembleRelease
```

## API

Rates are fetched from the CBRF public XML feed — no API key required.

| Endpoint | Description |
|----------|-------------|
| `GET https://www.cbr.ru/scripts/XML_daily.asp?date_req=DD/MM/YYYY` | Daily rates (omit param for today) |
| `GET https://www.cbr.ru/scripts/XML_val.asp` | Currency list |
| `GET https://www.cbr.ru/scripts/XML_val.asp?d=1` | Currency list (D1) |
