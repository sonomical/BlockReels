# Contributing to BlockReels

Use GitHub Issues for ordinary bugs and feature requests. For a security or
privacy vulnerability, follow [SECURITY.md](SECURITY.md).

## Set up and test

Use JDK 17 and the Android SDK. From the project root, run:

```sh
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
```

When a change affects Accessibility, detection, or blocking overlays, include
any physical-device testing you were able to do. Check both a short-video feed
that should be interrupted and an ordinary screen, such as messaging or search,
that should remain usable.

## Open a pull request

1. Fork the public repository and make a branch from `main`.
2. Keep the change focused. Add or update a regression test when you fix a
   behavior that existing tests do not cover.
3. Describe the problem, what changed, and which checks you ran. For detection
   bugs, include the Android version, affected app and version, expected result,
   and actual result.

Keep app behavior offline and preserve the
[privacy policy](docs/privacy-policy.md). Contributions to the project are made
under its MIT License.
