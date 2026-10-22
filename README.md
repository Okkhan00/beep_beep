# Media Download Manager Pro

Native Android app (Kotlin, Jetpack Compose, Material 3) for downloading **publicly accessible** media with
yt-dlp and FFmpeg, entirely on-device. Based on [iam-sandipmaity/video-downloader](https://github.com/iam-sandipmaity/video-downloader).

**License:** GPL-3.0 (see `LICENSE`, `THIRD_PARTY_NOTICES.md`). Upstream MIT notice preserved in `LICENSE-MIT-UPSTREAM.txt`.

## What it does
- Paste a URL or share a link to the app (Share Sheet).
- Reads metadata, lets you pick quality/format, shows progress, speed and size.
- Queue with pause / resume / cancel / retry and restart recovery; foreground-service notification.
- History, library and file management; saves via MediaStore / SAF.

## What it deliberately does not do
- No DRM, paywall, login or platform-restriction bypass.
- No cookie import, in-app login browser, account credentials or session harvesting.
- No YouTube sign-in / PO-token workarounds.
- No in-app APK self-updater (the `REQUEST_INSTALL_PACKAGES` permission was removed).
- Content that needs a login is reported as unsupported.

Only download content you own or have permission to download, and respect each site's terms and your local law.

## Build
Built by GitHub Actions (`.github/workflows/android-build.yml`) with the committed Gradle wrapper:
`./gradlew :app:assembleStandardDebug`. The debug APK is uploaded as the `media-downloader-debug-apk` artifact.
Unit tests and lint run in the same workflow.

- ABI: arm64-v8a only in this version. 32-bit (armeabi-v7a) is not claimed because it has not been tested.
- yt-dlp is pinned and SHA-256 verified at build time; FFmpeg comes from the pinned official
  `youtubedl-android:ffmpeg:0.18.1` module.

See `docs/PHASE2_AUDIT.md` for the audit and `docs/BUILD_REPORT.md` for the build report.
The previous upstream README is kept at `docs/UPSTREAM_README.md`.
