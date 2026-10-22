# Licensing and third-party notices

## Distribution license: GPL-3.0-or-later

This application is distributed under the **GNU General Public License v3.0** (see `LICENSE`).

Reason: the app links `youtubedl-android` (GPL-3.0), and bundles builds of Python/yt-dlp
tooling that are GPL-compatible. A combined work that includes a GPL-3.0 library must be distributed
under GPL-3.0 terms. Anyone who receives the APK is entitled to the corresponding source, which is this
repository at the commit used for the build.

## Original work

The foundation of this project is `iam-sandipmaity/video-downloader`, MIT License,
Copyright (c) 2026 Sandip Maity. The MIT text is preserved unchanged in `LICENSE-MIT-UPSTREAM.txt`.
MIT code may be relicensed as part of a GPL-3.0 combined work; the copyright notice stays.

## Redistributed components

| Component | License | Status |
|---|---|---|
| youtubedl-android `library` 0.18.1 (JunkFood02 fork) | GPL-3.0 | verified from source repo LICENSE |
| youtubedl-android `ffmpeg` 0.18.1 | GPL-3.0 for the wrapper; the FFmpeg binary inside is built from FFmpeg, whose licence depends on the configure flags used by the module's build | **NOT VERIFIED**: Maven Central is unreachable from the authoring session. Verify with the CI APK-inspection step and the module's `META-INF`/POM before any public release |
| yt-dlp 2026.10.21 (script fetched at build time, SHA-256 verified against the release `SHA2-256SUMS`) | Unlicense | verified at project level |
| Mozilla CA bundle `cacert.pem` (curl.se, SHA-256 verified) | MPL-2.0 | by publisher statement |
| AndroidX, Jetpack Compose, Media3, WorkManager, Room, DataStore | Apache-2.0 | by publisher convention; per-artifact check not done |
| Hilt / Dagger | Apache-2.0 | same |
| Coil | Apache-2.0 | same |
| kotlinx.coroutines / kotlinx.serialization | Apache-2.0 | same |
| com.google.android.material | Apache-2.0 | same |
| org.tukaani:xz | 0BSD / public domain | same |

## Components removed because licensing was unclear

The earlier build downloaded a custom FFmpeg (`iam-sandipmaity/video-downloader-packages`) that had no
license file, included GPL/LAME codecs and was fetched without a checksum. It is **no longer bundled**.
FFmpeg now comes from the pinned official `youtubedl-android:ffmpeg` module.

## Obligations when you distribute an APK

1. Provide the corresponding source (this repository at the build commit) and this notice file.
2. Keep `LICENSE`, `LICENSE-MIT-UPSTREAM.txt` and this file intact.
3. Do not add terms that restrict GPL freedoms.
4. If you publish on a store that is incompatible with GPL-3.0 terms, do not use it.

This document is engineering guidance, not legal advice.
