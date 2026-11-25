# Changelog

All notable changes to BlockReels are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added
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
