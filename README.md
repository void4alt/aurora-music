# Aurora Music

A native Android music discovery and playback app built with Kotlin, Jetpack Compose, Material 3, Media3/ExoPlayer, Room, Retrofit, Hilt, Coroutines, Flow and DataStore. Dark by default, original branding, no WebView.

## Unlimited, key-free YouTube access

Aurora does **not** depend on your own YouTube Data API key and does **not** have a per-user daily quota.

- **Discovery (search, trending, suggestions)** uses YouTube's own InnerTube endpoints with the public web client — no `YOUTUBE_API_KEY` needed, no 10 000-unit/day ceiling.
- **Playback** resolves each video through the InnerTube `player` endpoint (ANDROID / VR clients) and plays the returned direct audio stream inside ExoPlayer. If a client withholds a plain URL, the resolver falls back to public Piped and Invidious instances, which serve proxied audio streams.
- **No ads** — direct audio streams are requested and played; the ad-laden web/iframe player is never used.
- **No key, no quota, no WebView, no YouTube app handoff.** Background playback, notification and lock-screen controls all work.

Resolved URLs are cached for one hour. Queue transitions re-resolve automatically through a `ResolvingDataSource`, so old URLs never kill a playlist. Every resolved URL is range-checked before it is handed to ExoPlayer, and playback failures surface as a snackbar instead of dying silently.

## Look and feel

- Animated aurora gradients behind every screen.
- Aurora the cat: a Canvas-drawn mascot that bobs, blinks, sways its tail, wears headphones and floats music notes while a track plays.
- Pulsing album art, animated equalizer bars, staggered song-row entrances, springy play button, slide-up player screen and snackbar feedback.

## Features

- Search videos by title, artist and category; discover trending music.
- In-app audio playback from YouTube, direct audio URLs and local files.
- Background playback through a `MediaSessionService` foreground service with a media playback foreground-service type.
- Lock-screen and notification controls via Media3 `MediaSession` and `MediaStyle` notifications.
- Playlists, favorites and listening history persisted in Room.
- Add any song to a playlist from Home, Search, Explore, Library and the full player; create playlists on the fly.
- Import a YouTube playlist or video link in Library — paste a `youtube.com/playlist?list=...` link and every track becomes a playable song. Both compact and full playlists are followed across continuation pages.
- Player features: artwork, seek, shuffle, repeat, queue, speed, sleep timer, mini-player, full-screen player.
- Settings persisted with DataStore: recent searches, playback speed, sleep timer, shuffle, repeat, audio quality.

## Project structure

```
app/src/main/java/com/auramusic/
├── AuroraApplication.kt
├── models/          Domain models
├── database/        Room entities, DAOs, database
├── network/         InnerTube client, audio stream models, duration parsing
├── data/            Repository implementations, stream resolver, mappers
├── di/              Hilt modules and qualifiers
├── settings/        DataStore
├── playback/        MediaSessionService, resolving data source, custom commands
├── player/          MediaController connection, media item mapping, PlayerViewModel
└── presentation/    Activity, theme, navigation, components, screens + ViewModels
```

## Setup

1. Install **Android Studio** with **Android SDK Platform 34**, **Build Tools 34.0.0** and **JDK 17**.
2. Copy `local.properties.example` to `local.properties` and set `sdk.dir` to your Android SDK path. No API key is required.
3. Open the folder, let Gradle sync, run the `app` configuration on Android 8.0 (API 26) or newer.

## Build from the command line

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

APKs land in `app/build/outputs/apk/debug/` and `app/build/outputs/apk/release/`. Ready-to-install copies are also in `artifacts/`.

The included `app/keystore/aurora-release.jks` is a **self-signed demo signing key**; replace it before publishing.

## Permissions

`INTERNET`, `ACCESS_NETWORK_STATE`, `WAKE_LOCK`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS` (runtime on Android 13+), `READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE` (maxSdk 32).

## Notes

- An internet connection is required to resolve YouTube audio; local files and direct URLs work offline.
- Public Piped/Invidious instances can come and go; the resolver tries several in sequence.
- Original code written for this project. No YouTube Music branding, logos or proprietary assets are used.
