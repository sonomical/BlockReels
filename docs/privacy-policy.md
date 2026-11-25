# BlockReels Local Privacy Policy

BlockReels operates strictly offline. The APK does not declare `android.permission.INTERNET`, has no accounts or telemetry, and never transmits screen data, settings, or activity records off-device.

## Accessibility Service Data Handling

Android Accessibility delivers window events for enabled target packages (Instagram, YouTube, TikTok/Douyin, Facebook, and Snapchat). When protection is active:

- In **Feed Only** scope, BlockReels inspects window identity, view hierarchy structure, bounds, resource IDs, text, and content descriptions strictly in memory to distinguish short-video feeds (Reels, Shorts, TikTok, Spotlight) from ordinary areas such as messages, profiles, search, and long-form videos.
- In **Full App** scope, BlockReels matches the foreground package name without deep feed inspection.
- Based on your configuration, BlockReels triggers Android Back navigation, Home navigation, or an on-device blocking/mindful-pause overlay.

## Local Storage

- Activity logs record event IDs, timestamps, app package labels, intervention modes, and fixed diagnostic codes. Captions, typed text, messages, and view trees are never persisted.
- Rules, engine parameters, schedules, daily pass counters, preferences, and salted PBKDF2-SHA256 PIN hashes are stored locally in Android DataStore / SharedPreferences.
- Optional JSON backups copy configuration (rules, engine parameters, and schedules) to the device clipboard; PIN credentials and activity logs are excluded.

## User Control

An explicit disclosure confirmation is required before opening Android Accessibility Settings. You can disable the Accessibility service at any time in Android Settings, clear local activity history in-app, or remove all stored data via Android App Info.
