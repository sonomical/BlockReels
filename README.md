# BlockReels

BlockReels is an offline, native Kotlin + Jetpack Compose (Material Design 3) Android app that selectively interrupts short-form video feeds on Instagram, YouTube, Facebook, and TikTok while keeping ordinary areas such as messaging, search, and long-form video usable.

## Architecture

- **100% Native Kotlin & Jetpack Compose (Material 3)**: Zero cross-platform runtime overhead, dynamic System/Light/Dark/AMOLED themes, bundled local typography, and adaptive phone/tablet layouts.
- **On-Device Accessibility Engine**: Multi-layer detectors (`InstagramDetector`, `YouTubeDetector`, `TikTokDetector`, `FacebookDetector`), `DisambiguationEngine`, `TraversalEngine`, `ScreenScanQueue`, and `InterventionController` with audio ducking/mute and hold-to-unlock overlay.
- **Zero-Network Privacy**: `android.permission.INTERNET` is not declared anywhere in the app. All detection, rules, schedules, PBKDF2-SHA256 PIN hashing, and analytics run strictly on-device.

## Build & Test

Requires JDK 17 and the Android SDK:

```sh
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
```

See [docs/privacy-policy.md](docs/privacy-policy.md) for privacy details and [LICENSE](LICENSE) for license terms.
