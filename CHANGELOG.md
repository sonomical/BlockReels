# Changelog

All notable changes to BlockReels are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

## [1.1.0-beta.1] - 2026-11-25

### Added
- **GitHub Actions CI/CD pipelines**: automated unit test + minified release verification (`android-ci.yml`), Beta Pre-Release CD (`beta-release.yml` on `beta` branch, `v*-beta*` tags, or manual trigger with optional Google Play beta track upload), and Production Release CD (`release.yml`).
- **Expanded blocking policies & daily goals**: block Reels/Shorts, Main Feed, Both, or hide surfaces automatically, configure separate daily minute goals for Reels and Feed, and apply rules across all supported apps in one tap.
- **Range-validated numerical controls with quick presets**: one-tap presets paired with precision steppers, sliders, and direct numeric input dialogs across goals, hold duration, pass length, daily cap, entry delay, per-app overrides, and custom Focus Lock durations.
- **Appearance & Theme Studio**: System/Light/Dark/AMOLED modes, Android 12+ Material You dynamic color, six curated opaque Material 3 Expressive color palettes, contrast levels, corner geometry styles, typography scaling, and strictly theme-colored social app icon styles (Cookie, Squircle, Soft Circle).
- **Four-step editorial Material 3 Expressive onboarding** with custom illustrations, animated step progress, target selection, policy/goal setup, and live permissions checklist.
- **Editorial Block Message screen** with hero illustration, bold typography, inline headline/body editing, and a clear breakdown of triggered actions.

### Changed
- Adopted an opaque Material 3 Expressive floating bottom navigation bar on compact screens and `NavigationRail` on expanded screens.
- Bumped backup codec to schema v4 (backward-compatible with v1–v3) and app version to `1.1.0-beta.1` (`versionCode = 2`).

### Earlier Unreleased Changes
- **Home Exit (`REDIRECT_HOME`)** intervention mode alongside Auto Back, Mindful Pause, and Strict Block.
- **Blocking Scope (`Feed Only` / `Full App`)** per target app and per schedule override.
- **Per-app rule overrides** for mode, scope, hold duration, pass length, and daily pass quota.
- **Detection sensitivity presets** (`Conservative`, `Balanced`, `Aggressive`) controlling confidence thresholds and scan cadence.
- **Entry grace delay** (`0s`, `3s`, `5s`, `10s`) before intervention fires.
- **Configurable temporary pass duration (`1–15m`)**, **daily pass quota (`Off`, `1`, `3`, `5`, `10`)**, and instant pass revocation from the Shield screen.
- **Audio policy** (`Silence`, `Duck`, `Off`) replacing the legacy boolean mute switch.
- **Schedule action and scope overrides** plus Focus Lock mode selection.
- **Snapchat Spotlight** (`com.snapchat.android`) and **Douyin** (`com.ss.android.ugc.aweme`) target support.
- **Backup schema v3** with backward compatibility for v1–v2 backups.
- Open-source repository templates, CI workflow, and unit test suite.

### Changed
- Reworked app navigation and screens (`Shield`, `Engine`, `Schedules`, `Activity`, `Settings`) to surface blocking engine controls directly with zero filler cards or prose.
- Streamlined onboarding from 4 pages to 3 essential steps.
- Updated local build and verification scripts to target the native root Gradle Android project directly.

## [1.0.0] - 2026-03-01

### Added
- Initial native Kotlin + Jetpack Compose (Material 3) offline short-video blocker for Instagram Reels, YouTube Shorts, TikTok, and Facebook Reels.
