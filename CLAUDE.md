# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

OverlayDictionary is an Android app that renders a floating overlay window on top of other apps, letting users type text and receive English→Portuguese translations via the [MyMemory API](https://api.mymemory.translated.net/).

## Build Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew test                   # Run unit tests
./gradlew connectedAndroidTest   # Run instrumented tests (device/emulator required)
./gradlew lint                   # Run Android Lint
./gradlew clean                  # Clean build artifacts
```

## Architecture

Single-module app (`app/`) with no MVVM/MVI framework — architecture is intentionally minimal.

### Key Components

- **`MainActivity`** — Checks `SYSTEM_ALERT_WINDOW` permission, requests it if missing, then starts `OverlayService`.
- **`OverlayService`** — Android `Service` that creates a floating `View` via `WindowManager` (`TYPE_APPLICATION_OVERLAY`, `Gravity.BOTTOM`). Hosts an `EditText`, a result `TextView`, and a translate `Button`. Makes HTTP calls to the MyMemory API on a background `Thread` and posts results back to the UI thread via `Handler(Looper.getMainLooper())`.
- **`overlay_bar.xml`** — XML layout for the floating window (60dp × 120dp `FrameLayout`).
- **`ui/theme/`** — Material 3 theme with dynamic colors (Android 12+).

### Network

Translation endpoint (hardcoded in `OverlayService`):
```
https://api.mymemory.translated.net/get?q={text}&langpair=en|pt
```
No Retrofit or OkHttp — raw `HttpURLConnection` on a plain `Thread`.

### Manifest Permissions

- `SYSTEM_ALERT_WINDOW` — required to draw the overlay
- `INTERNET` — required for translation API calls

## Tech Stack

| Layer | Library |
|-------|---------|
| UI (Activity) | Jetpack Compose + Material 3 |
| UI (Overlay) | Android XML Views + `WindowManager` |
| Async | `Thread` + `Handler` (no Coroutines) |
| DI | None |
| Testing | JUnit 4, Espresso, Compose UI Test |

**Build toolchain**: Gradle 8.9 (Kotlin DSL), AGP 8.7.3, Kotlin 2.0.0, Java 11, compile SDK 34, min SDK 24.  
Dependencies are managed via the version catalog at `gradle/libs.versions.toml`.
