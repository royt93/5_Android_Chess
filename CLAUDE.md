# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Quick Chess — a single-module Android chess game (PvP local + PvE vs AI) by SAIGON PHANTOM LAB. The codebase is a **mix of Java (chess engine + activities) and Kotlin (Application, base activity, language + ads glue)**. Min SDK 24, target/compile SDK 37, Java 8 / Kotlin 2.2.0, AGP 8.12.0.

> Note: `applicationId` and the launcher package are `com.saigonphantomlabs`, but the Gradle `namespace` is `com.saigonphantomlabs.chess`. Therefore `R` and `BuildConfig` are generated as **`com.saigonphantomlabs.chess.R`** / **`com.saigonphantomlabs.chess.BuildConfig`** — import them from there, not from the app package.

## Build & Run

```bash
./gradlew assembleDebug       # debug APK
./gradlew assembleRelease     # signed release APK (needs signing props, see below)
./gradlew bundleRelease       # AAB for Play Store
./gradlew clean
./gradlew lint                # Android Lint
./gradlew installDebug        # build + install on connected device/emulator
```

There are **no unit or instrumentation tests** — `app/src/test` and `app/src/androidTest` exist but are empty (an `AndroidJUnitRunner` is declared but unused). Verification is manual via the app.

Release signing reads `KEYSTORE_FILE` / `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD` from `gradle.properties` (committed in this repo) or `-P` flags.

## Architecture

### Activity flow
`SplashActivity` (LAUNCHER) → `MainActivity` (menu) → `ChessBoardActivity` (game). `PawnPromotionActivity` is launched modally during promotion. From the menu, **PvP** starts directly; **PvE** first shows a difficulty dialog and passes the chosen `AIEngine.Difficulty` to `ChessBoardActivity` via Intent extra (string parsed back with `Difficulty.valueOf`, defaulting to `EASY`).

All visible activities extend **`BaseActivity`** (Kotlin), which applies the saved locale in `attachBaseContext` — any new activity must extend it or localization breaks.

### Chess engine (`com.saigonphantomlabs.chess`, Java)
- **`Chess.java`** — the central game controller (~1100 lines). Owns the `Chessman[8][8]` board, whose-turn state, move validation orchestration, captured-piece list, undo stack, sound (`SoundPool`) + haptics, and all the piece/turn animations. Also drives the AI: when it's Black's turn in PvE it sets `isAiThinking`, asks `AIEngine` for a move off the main thread, then applies it. `isAiThinking` gates user input and Undo/Restart.
- **`Chessman.java`** — abstract base for pieces; subclasses `King`, `Queen`, `Rook`, `Bishop`, `Knight`, `Pawn` each implement their move generation. Holds piece color (`PlayerColor`), type, and per-piece animation helpers.
- **`AIEngine.java`** — self-contained minimax + alpha-beta with piece-square tables and material values; `Difficulty` enum `EASY / MEDIUM / HARD / UNBEATABLE` controls search depth and think delay. The human plays **White**; the AI plays **Black**.
- **`Point.java`**, **`MoveRecord.java`** — board coordinates and move history records (used for undo + AI).

### State passing & persistence
- **`Storage.java`** — a static holder using a `WeakReference<Chess>` to hand the live `Chess` instance between activities (deliberately instead of `startActivityForResult`), plus `Storage.result` for the chosen promotion piece. `ChessBoardActivity` creates/stores the `Chess` on first launch and clears it on finish; the promotion activity writes back into `Storage.result`. Be careful with its lifecycle — it's cleared in several `onDestroy`/exit paths.
- **`BoardThemeManager.java`** — 20 light board themes (5 families × 4), persisted via `SharedPreferences`; renders board square bitmaps. `BoardThemePickerDialog` is the picker UI.
- **`GameStatsManager.java`** — win/loss/game stats in `SharedPreferences`.
- **`LanguageManager`** (Kotlin) — stores `pref_language` in default `SharedPreferences` (default `"en"`), applies the `Locale` via `createConfigurationContext`. Translations live in `res/values-*` (de, es, fr, id, ja, km, ko, lo, ms, pt, th, tr, vi, zh). `LanguageBottomSheet` is the in-app picker.

### Rendering
**`PieceRenderer.java`** applies runtime pseudo-3D effects (drop shadow, rim light, specular) to flat 2D piece PNGs, caching results in an `LruCache` keyed by piece type + color. Pieces are colored/tinted at runtime rather than shipped per-color.

### Ads (`MyApplication.kt`)
Ads go through the external **AdmobWrapper SDK** (`com.github.royt93:AdmobWrapper:1.1.1` via jitpack, package `com.roy.sdkadbmob.*` — `AdManager`, `UIUtils`). `MyApplication.onCreate` builds an `AdSdkConfig` from `BuildConfig` ad-ID fields and calls `AdManager.setConfig(...)`. The flag **`BuildConfig.IS_ENABLE_ADMOB`** (currently `false`) switches the active network: `false` → AppLovin MAX, `true` → AdMob. Touchpoints: App Open (Splash), Banner (`ChessBoardActivity`), Interstitial (on Undo/Restart/end-game). Ad unit IDs differ between `debug` (Google test IDs) and `release` build types — see `app/build.gradle`. **Do not add new ad touchpoints without explicit instruction** (see `doc/AD.MD`).

## Conventions & gotchas
- New activities → extend `BaseActivity`; import `R`/`BuildConfig` from `com.saigonphantomlabs.chess`.
- Click handlers use **`SafeClickListener`** (debounce against double-taps) rather than raw `OnClickListener`.
- Release builds run **R8 full mode + resource shrinking**; if you add reflection/serialization or keep classes, update `proguard-rules.pro` (release) / `proguard-rules-debug.pro` (debug). APK size is an explicit project goal — favor lightweight additions.
- `viewBinding` is enabled; AIDL/RenderScript/resValues/shaders are disabled.
- `leakcanary` is wired in debug builds — watch for leaks when touching activity/animation lifecycles; `Chess` holds view references and animators that must be cancelled/cleared on destroy.

## Planning docs
The `doc/` folder holds Vietnamese planning notes that are useful context: `AD.MD` (ad SDK migration plan & rules), `reskin.md` (UI redesign plan), `game_rule.md` (chess rules), `memory_leak.md`, `quick_win.md`, `reskin.md`.
