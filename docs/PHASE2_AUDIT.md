# Phase 2 audit: video-downloader (commit 4469e1e, 2026-09-27)

Method: shallow clone and source inspection. Nothing here was compiled; the session cannot reach Gradle, Google Maven or Maven Central.

## Toolchain (from the repo, not yet build-verified)
- AGP 9.2.1, Gradle wrapper 9.5.1, Kotlin 2.3.21, KSP 2.3.7, JDK 17 in CI
- compileSdk/targetSdk 36, minSdk 26, Hilt 2.59.2, Room 2.8.4, WorkManager 2.11.2
- youtubedl-android `library` 0.18.1 (Maven Central, published 2025-11-16). The 0.17.2 I cloned earlier was only the newest tag on the GitHub repo; Maven lists 0.17.3 through 0.18.1. Use 0.18.1 as the repo does.

## Licensing
| Component | License | Status |
|---|---|---|
| App source | MIT | verified (LICENSE) |
| youtubedl-android | GPL-3.0 | verified (LICENSE in repo clone) |
| yt-dlp source | Unlicense | verified; PyInstaller builds are GPLv3+ |
| Custom FFmpeg (video-downloader-packages) | UNCLEAR | repo has no LICENSE; build includes libx264 (GPL) and LAME; exact configure flags unseen; arm64 only; fetched at build time with no checksum |
| yt-dlp script fetched at build time | Unlicense | fetched as "latest" with no checksum, so not reproducible |
| AndroidX, Compose, Hilt, Room, Coil, Media3, kotlinx | Apache-2.0 | by publisher convention; not yet checked per artifact |
| com.google.android.material | Apache-2.0 | same |
| org.tukaani:xz | public domain / 0BSD | same |

Consequence: the distributed app must be GPL-3.0 (youtubedl-android). The custom FFmpeg must not be bundled until its license is resolved.

## Manifest and share sheet
- `QuickDownloadActivity` handles `ACTION_SEND` for `text/plain` and `text/uri-list` (exported). Share-sheet intake exists.
- `MainActivity` handles `VIEW` for local `video/*` and `audio/*`.
- Permissions to remove: `REQUEST_INSTALL_PACKAGES`, `USE_BIOMETRIC`, `USE_FINGERPRINT`. Review: `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (Play-restricted; keep only if justified), `READ_MEDIA_AUDIO`.
- Missing: `READ_MEDIA_VIDEO` is not declared; check whether the library needs it.

## Features to remove (user decision)
Cookie import, in-app browser, YouTube access recovery / PO-token generation, biometric gate, app self-updater. Files involved (about 40): BrowserScreen, CookiesScreen, YoutubeAuthScreen, AccessSettingsScreen, CookieProfile, CookieTextCodec, WebViewCookieExporter, WebViewSessionSanitizer, TrustedWebViewConfigurer, YoutubePoToken*, YoutubeAuthConfig, DownloadOptionSecretsStore, BiometricHelper, AppUpdateManager, ApkSignatureVerifier, plus references in DownloadEngine, FormatExtractor, YoutubeRequestPlanner, DownloadRepositoryImpl, DownloadWorker, SettingsStore, AppSettings, DownloaderApp and ViewModels.

## FFmpeg and ABI
- Current FFmpeg exists for arm64-v8a only (COMPATIBILITY.md), so armeabi-v7a cannot be claimed.
- Candidate replacement: `io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1` (Termux-derived, multi-ABI, pinned on Maven Central). Its exact license and ABI contents are NOT yet verified because Maven is unreachable from this session.

## Not audited
- Open issues and PRs (GitHub API not available to this session).
- Per-artifact licenses beyond the table.
- Runtime behavior of queue, pause/resume and restart recovery (needs device).
