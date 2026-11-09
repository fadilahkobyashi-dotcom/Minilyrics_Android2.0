# MiniLyrics for Android

A modernized revival of the classic **MiniLyrics** music player and synchronized lyrics workstation, engineered with modern Android architecture (Jetpack Compose, Kotlin DSL, Coroutines, Room, Material 3).

## Features

- **Synchronized Lyrics Display**: Auto-scrolling, tap-to-seek, and timing adjustments (`-0.5s` / `+0.5s` offset calibration).
- **Multi-Source Online Lyrics Search**: Queries LRCLIB and Netease Cloud Music lyrics providers with automatic fallback and manual title/artist correction.
- **On-Device Romanization & Transliteration**:
  - Japanese Kanji & Kana to Hepburn Romaji.
  - Mandarin Hanyu Pinyin with tone indicators.
  - Korean Hangul to Revised Romanization.
  - Three lyrics view modes: Original, Romaji, and Dual-Lyrics.
- **Local Audio Playback & MediaSession**:
  - Full foreground playback service (`MediaPlaybackService`) with notification controls and headset noisy receiver.
  - Seamless device library scanning and bundled demonstration tracks.
- **Audio Effects & Hardware Equalizer**:
  - 5-band equalizer with 10 presets (Rock, Pop, Jazz, Bass Boost, Treble Boost, etc.).
  - Hardware Bass Boost and Virtualizer effects.
- **12 Custom Themes**:
  - Classic Charcoal (Vintage MiniLyrics aesthetic), AMOLED Black, Light Mode, Sakura Pink, Ocean Blue, Sunset Orange, Purple Night, Forest Green, Cyberpunk Neon, Minimalist, Glassmorphism, and Gradient.
- **Persistent Library & Custom Playlists**:
  - Favorites, Recently Played, Most Played, and custom user playlists powered by Room DB.
  - Embedded ID3 artwork and album covers caching.

## Tech Stack

- **UI**: Jetpack Compose, Material 3
- **Language**: Kotlin 2.x
- **Build**: Gradle Kotlin DSL, AGP 9.x, Android SDK 36 (Android 16 compatible)
- **Local Database**: Android Room with KSP
- **Networking**: Retrofit 2, OkHttp 3, Moshi
- **Preferences**: Jetpack DataStore Preferences
- **Image Loading**: Coil 3
