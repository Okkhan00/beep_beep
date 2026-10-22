# Build report: first debug APK

Source of truth: GitHub Actions run 39714223578 on `Okkhan00/beep_beep` (workflow "Android Build"), result **success**.

## Executed in CI (verified)
| Check | Result |
|---|---|
| `./gradlew :app:assembleStandardDebug` | success |
| Debug APK produced and uploaded (`media-downloader-debug-apk`) | 86 MB |
| Unit tests (`:app:testStandardDebugUnitTest`) | 118 tests, 0 failures, 0 errors, 0 skipped |
| APK ABI folders | `lib/arm64-v8a/` only |
| Native libs in APK | libffmpeg.so, libffmpeg.zip.so, libpython.so, libpython.zip.so, libqjs.so (all arm64-v8a) |
| `REQUEST_INSTALL_PACKAGES` in merged manifest | absent |
| Lint (`:app:lintStandardDebug`) | **failed**: 28 errors (NewApi 1, RestrictedApi 15, UnsafeOptInUsageError 12), 14 warnings. Lint is advisory in this workflow. Not compared against the unmodified baseline, so it is not known whether these predate this work |

## Not executed (needs a device)
Everything about runtime behavior: download success per site, queue/pause/resume/cancel/retry, restart recovery, foreground-service notification behavior, MediaStore/SAF saving, FFmpeg merge/convert, share-sheet intake, Vault biometrics, UI appearance. None of this has been run.

## Changes in this version
- Removed: cookie import, in-app login browser/WebView cookie export, YouTube access/PO-token generation, cookie-backed retries, app self-updater and APK installer, FFmpeg overlay updater, `REQUEST_INSTALL_PACKAGES`.
- Kept: yt-dlp updater (SHA-256 verified), Private Vault (so `USE_BIOMETRIC`/`USE_FINGERPRINT` remain), `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (user-triggered, for background download reliability).
- FFmpeg: custom unlicensed, unchecksummed download replaced by `io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1`.
- yt-dlp pinned to 2026.10.21 and verified against the release SHA2-256SUMS; CA bundle verified against curl.se's published SHA-256.
- License switched to GPL-3.0 (see `THIRD_PARTY_NOTICES.md`).
- Old persisted download options still load (`DownloadOptionsCodec` ignores removed fields); stored cookie/auth settings are deleted on first settings write.

## Known limitations
- arm64-v8a only. armeabi-v7a is not claimed because it is untested.
- Debug APK only; signed with the default debug key. No release build was made.
- FFmpeg module licensing details (codec configuration) are unverified; confirm before any public distribution.
- Translated strings for the removed features remain in locale files, unused. Recovery-guidance strings that mentioned cookies were rewritten in English; the other-locale versions were removed so they fall back to English.
- Open upstream issues were not audited.
- The sandbox cannot download CI artifacts or logs; download the APK from the Actions run page.
