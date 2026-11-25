# BlockReels

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

BlockReels is an offline, open-source Android app written in native Kotlin and Jetpack Compose (Material 3) that intercepts short-form video feeds while keeping messaging, search, profiles, and long-form content usable.

## Blocking Engine

| Capability | Options |
| --- | --- |
| **Intervention Modes** | `Auto Back` (navigate back), `Home Exit` (jump to launcher), `Mindful Pause` (hold-to-pass overlay), `Strict Block` (no temporary bypass) |
| **Blocking Scope** | `Feed Only` (selective short-video feed detection) or `Full App` (immediate block on launch), configurable globally per target or per schedule |
| **Per-App Overrides** | Independent mode, scope, hold duration, pass length, and daily pass cap per target app |
| **Detection Sensitivity** | `Conservative` (strict threshold, 150ms), `Balanced` (standard, 75ms), `Aggressive` (rapid, 40ms) |
| **Entry Grace Delay** | `0s`, `3s`, `5s`, or `10s` grace window before triggering intervention |
| **Temporary Passes & Quotas** | Hold duration `3–30s`, pass length `1–15m`, daily pass cap (`Off`, `1`, `3`, `5`, `10`), and instant pass revocation from the Shield tab |
| **Schedule Overrides** | Recurring weekday/overnight windows and timed Focus Lock boosts with independent action and scope overrides |
| **Audio & Haptics** | `Silence` (mute media stream), `Duck` (lower volume via audio focus), or `Off`, plus optional tactile feedback |
| **Anti-Tamper** | Salted PBKDF2-SHA256 4-digit PIN lock and Global Strict Lock |

## Supported Targets

- **Instagram** (`com.instagram.android`) — Reels
- **YouTube** (`com.google.android.youtube`) — Shorts
- **TikTok** (`com.zhiliaoapp.musically`, `com.ss.android.ugc.trill`) — For You feed
- **Douyin** (`com.ss.android.ugc.aweme`) — Short-video feed
- **Facebook** (`com.facebook.katana`, `com.facebook.lite`) — Reels
- **Snapchat** (`com.snapchat.android`) — Spotlight

## Architecture & Privacy

- **Zero-Network Guarantee**: `android.permission.INTERNET` is never declared in `AndroidManifest.xml`. All detection, schedules, PIN hashing, backups, and activity counters stay strictly on-device.
- **Native Accessibility Engine**: Dedicated per-platform detectors (`InstagramDetector`, `YouTubeDetector`, `TikTokDetector`, `FacebookDetector`, `SnapchatDetector`), `DisambiguationEngine`, `TraversalEngine`, `EventThrottler`, `InterventionConfigResolver`, and `InterventionController`.
- **Offline JSON Backup**: Export and restore settings, per-app rules, and schedules via clipboard JSON (schema v3, backwards-compatible with v1–v2; PIN credentials are never exported).

See [docs/privacy-policy.md](docs/privacy-policy.md) for details.

## Build & Test

Requires **JDK 17+** and the **Android SDK** (compileSdk 36, minSdk 24):

```sh
# Run unit tests
gradle :app:testDebugUnitTest

# Build debug APK
gradle :app:assembleDebug

# Run local verification (unit tests, debug APK, zero-network permission audit)
./scripts/verify-local.sh
```

## Contributing

Contributions are welcome under the [MIT License](LICENSE). See [CONTRIBUTING.md](CONTRIBUTING.md), [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md), and [SECURITY.md](SECURITY.md).

## License

Copyright (c) 2026 BlockReels Contributors. Released under the [MIT License](LICENSE).
