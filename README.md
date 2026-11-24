# Aurora Music

A native Android music discovery and playback app built with Kotlin, Jetpack Compose, Material 3, Media3/ExoPlayer, Room, Retrofit, Hilt, Coroutines, Flow and DataStore. Dark by default, original branding, no WebView.

## What it does

- **Search** YouTube videos using the official YouTube Data API v3 (metadata only).
- **Discover** trending music, live, remixes and podcast-style content through search categories.
- **Play** real audio inside the app from **direct audio URLs** and **local audio files** using Media3/ExoPlayer.
- **Background playback** through a `MediaSessionService` foreground service with a media playback foreground-service type.
- **Lock-screen and notification controls** via Media3 `MediaSession` and `MediaStyle` notifications.
- **Playlists, favorites and listening history** persisted in Room.
- **Player features**: artwork, seek, shuffle, repeat, queue, speed, sleep timer, mini-player, full-screen player.
- **Settings** persisted with DataStore: recent searches, playback speed, sleep timer, shuffle, repeat, audio quality.

## Important: playback honesty

YouTube's official Data API returns **metadata, not playable media URLs**. Embedding/iframe playback is a web technology and cannot provide background playback, and extracting stream URLs is against YouTube's terms. Aurora therefore does two distinct things:

| Content type | Behaviour |
| --- | --- |
| YouTube search results (video metadata, thumbnails, channels, durations) | Shown in search/home/explore, saved to playlists and history. Tapping **Play** opens the official YouTube app via `ACTION_VIEW`. `isPlayableInApp = false`. |
| Direct audio URLs (http/mp3/m4a/HLS) and local files added from your device | Played **inside Aurora** with ExoPlayer, including background playback, notification and lock-screen controls. |

This is deliberate: the app never claims that YouTube-only metadata supports background audio.

## Project structure

```
app/src/main/java/com/auramusic/
├── AuroraApplication.kt
├── models/          Domain models
├── database/        Room entities, DAOs, database
├── network/         Retrofit API, DTOs, duration parsing
├── repository/      Repository contracts
├── data/            Repository implementations and mappers
├── di/              Hilt modules and qualifiers
├── settings/        DataStore
├── playback/        MediaSessionService, custom session commands
├── player/          MediaController connection, media item mapping, PlayerViewModel
└── presentation/    Activity, theme, navigation, components, screens + ViewModels
```

## Setup

1. Install **Android Studio** (Hedgehog or newer) with **Android SDK Platform 34**, **Build Tools 34.0.0** and a **JDK 17**.
2. Create a Google Cloud project, enable **YouTube Data API v3**, and create an API key.
3. Copy `local.properties.example` to `local.properties` and set:
   ```properties
   sdk.dir=/path/to/Android/sdk
   YOUTUBE_API_KEY=your_real_key_here
   ```
   `local.properties` is gitignored — never commit your key.
4. Open the project folder in Android Studio, let Gradle sync, then run the `app` configuration on a device or emulator running Android 8.0 (API 26) or newer.

If no key is configured, the app still builds and all local-library features work; search/trending display a clear configuration error.

## Build from the command line

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

APKs land in `app/build/outputs/apk/debug/` and `app/build/outputs/apk/release/`.

The included `app/keystore/aurora-release.jks` is a **self-signed demo signing key** and its password is in `local.properties.example`. Replace it with your own keystore before publishing.

## Permissions

`INTERNET`, `ACCESS_NETWORK_STATE`, `WAKE_LOCK`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS` (runtime on Android 13+), `READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE` (maxSdk 32). No contacts, location or other unnecessary permissions.

## Playback test checklist

1. Start a direct/local track, minimize the app — playback continues.
2. Open another app — audio continues; notification shows controls.
3. Turn the screen off — `WAKE_MODE_NETWORK` + foreground service keep it alive.
4. Lock the phone — lock-screen transport controls respond.
5. Notification play/pause/next/previous — routed through `MediaSession`.
6. Bluetooth connect/disconnect — `setHandleAudioBecomingNoisy(true)` pauses on noisy disconnect.
7. Incoming call — `AudioAttributes.DEFAULT` with audio focus handling ducks/stops playback.
8. Wi-Fi/mobile switch — ExoPlayer resumes with network error recovery.
9. Process recreation — queue metadata is restored from persisted library entries where available.

## Known limitations

- YouTube Data API quota is enforced by Google; heavy searching may hit the default daily quota.
- YouTube metadata items open the official YouTube app for playback rather than playing in-app.
- Local file URIs are persisted with `FLAG_GRANT_READ_URI_PERMISSION` where the provider allows it.
- Equalizer integration depends on device support and is not included; speed and sleep timer are.

## License

Original code written for this project. No YouTube Music branding, logos or proprietary assets are used.
