# Contributing to BlockReels

Thank you for contributing to BlockReels. Use GitHub Issues for bugs and feature requests, and follow [SECURITY.md](SECURITY.md) for security or privacy vulnerabilities.

## Development Setup

Requirements: **JDK 17+** and the **Android SDK** (`compileSdk = 36`, `minSdk = 24`).

```sh
# Run JVM unit tests
gradle :app:testDebugUnitTest

# Build debug APK
gradle :app:assembleDebug

# Run local verification and APK permission audit
./scripts/verify-local.sh
```

## Guidelines

- **Zero-Network Invariant**: Never add `android.permission.INTERNET`, telemetry SDKs, or remote network calls.
- **Selective Detection**: When modifying platform detectors or `DisambiguationEngine`, verify both the target short-video feed (should trigger) and ordinary surfaces such as DMs, search, profiles, and long-form videos (must remain usable).
- **Tests**: Add or update JUnit tests under `app/src/test/java/com/blockreels/app/` for engine, resolver, detector, schedule, or backup logic changes.
- **License Headers**: Include `// SPDX-License-Identifier: MIT` at the top of new Kotlin source files.

## Pull Requests

1. Fork the repository and create a branch from `main`.
2. Keep changes focused and run `gradle :app:testDebugUnitTest` and `gradle :app:assembleDebug` before opening a PR.
3. For detection fixes, note the Android version, target app version, expected behavior, and observed behavior.

By contributing, you agree that your contributions will be licensed under the project's [MIT License](LICENSE).
