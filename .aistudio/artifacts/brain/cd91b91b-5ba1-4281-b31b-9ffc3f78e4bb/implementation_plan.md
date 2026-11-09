# MiniLyrics: Android 16 Modernization & Feature Enhancement

A modernized, high-performance revival of the classic **MiniLyrics** music and synchronized lyrics player for Android 16, combining the nostalgic MiniLyrics UI layout with cutting-edge audio effects, multi-source online lyrics retrieval, on-device contextual Japanese Kanji-to-Romaji transliteration, and embedded album art extraction.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following technical directions have been aligned based on your preferences:

- **Confirmed Decision 1 (Online Lyrics Providers)**: Dual-source architecture prioritizing **LRCLIB** (high-accuracy synchronized LRC database) with automatic fallback and search via **Netease Cloud Music lyrics API**, supported by local LRC matching and manual search override.
- **Confirmed Decision 2 (Romanization Engine)**: On-device contextual transliteration engine with dictionary-assisted Japanese Kanji/Kana to Hepburn Romaji conversion, complemented by Korean (Revised Romanization) and Mandarin (Hanyu Pinyin) transliterators for Dual/Romaji lyrics display.
- **Confirmed Decision 3 (Audio Engine)**: Native Android **MediaPlayer** tightly bound to hardware-accelerated **Equalizer**, **BassBoost**, and **PresetReverb** audio effects, preserving audio quality and real-time response.

---

### 1. Overview & Core Concept

- **What It Does**: A complete local music player and synchronized lyrics workstation. It reads the device music library, extracts embedded album artwork and ID3 lyrics, queries online synchronized LRC lyrics when missing or messy, converts Japanese/Asian lyrics into Romaji in real-time, applies hardware equalizer presets, and displays lyrics across Original, Romaji, and Dual-Lyrics modes.
- **Visual Identity**: Strictly preserves the beloved classic MiniLyrics visual structure:
  - Header with tab bar: **Songs**, **Artists**, **Albums**, **Playlists**, and **Playback (Now Playing)**.
  - Classic MiniLyrics floating/docked lyrics display with active line highlighting, custom font size, text alignment, and time-offset nudging (`-0.5s` / `+0.5s`).
  - MiniLyrics title bar, track info header, album art vinyl/card frame, and transport controls.
  - 12 customizable theme presets (including Classic MiniLyrics Charcoal, AMOLED Black, Light Mode, Sakura Pink, Ocean Blue, Sunset Orange, Purple Night, Forest Green, Neon Cyberpunk, Minimalist, Glassmorphism, and Gradient).
- **Key Value**: Flawlessly solves the problem of corrupted filenames, unmatched lyrics, missing album covers, and unreadable Kanji lyrics on modern Android 16 devices.

---

### 2. User Experience & Visual Design

#### Navigation & Layout Structure
1. **Classic Top Tab Navigation**:
   - `Playback`: Full-screen synchronized lyrics viewer with album artwork backdrop, active line tracking, scrub bar, and transport controls.
   - `Songs`: Comprehensive track listing with search filter, duration, artist, and fast alphabetic scrollbar.
   - `Artists` & `Albums`: Grouped browsable library with cover art grid and nested track lists.
   - `Playlists`: Custom playlists, Favorites, Recently Played, and Most Played.
2. **Synchronized Lyrics Viewer**:
   - Smooth auto-scrolling with current timestamp highlight and progress reflection.
   - Tap-to-seek: Tapping any lyric line immediately jumps playback to that lyric timestamp.
   - Timing calibration buttons directly in the player bar (`[<] -0.5s`, `[>] +0.5s`, `Reset`).
   - Quick toggle button for **Original** | **Romaji** | **Dual** (showing original script and Romanized Latin script together).
3. **Smart Online Search & Title Correction Dialog**:
   - Automated title sanitization (strips `[FLAC]`, `(Official Video)`, `320kbps`, YouTube IDs, track numbers).
   - Manual metadata editor modal allowing users to edit Title, Artist, and Album on the fly.
   - Interactive search results sheet displaying Title, Artist, Source, Rating, LRC vs Plain TXT indicator, and an immediate **Apply & Save** button.
   - Saves persistent associations in Room DB so the song never loses its lyrics.
4. **Equalizer & Sound Effects Sheet**:
   - 5-Band Equalizer sliders with real-time dB adjustments.
   - Bass Boost wheel/slider and Virtualizer.
   - 10 Presets: Normal, Bass Boost, Treble Boost, Vocal Boost, Rock, Pop, Jazz, Classical, Dance, Custom.
5. **Settings & Themes Center**:
   - **Theme Selector**: Visual cards for 12 themes with live color previews.
   - **Lyrics Settings**: Font size slider, line spacing, text alignment (Left, Center, Right), highlight color picker, inactive line opacity.
   - **Storage & Cache**: Cache size counter, Clear Album Art Cache, Export Downloaded LRCs, Rescan Library.

---

### 3. Key Technical Decisions & Trade-Offs

- **Decision 1: Android 16 Media & Background Playback Architecture**
  - *Chosen Approach*: `MediaPlaybackService` running as an Android Foreground Service with type `mediaPlayback`, integrating `MediaSessionCompat` and rich `NotificationCompat.MediaStyle` controls (Play/Pause, Next, Previous, Favorite, and Dismiss).
  - *Why*: Ensures background audio never cuts off when switching apps or locking the device, and strictly satisfies Android 16 background execution rules.
- **Decision 2: Audio Effects Pipeline (Equalizer & BassBoost)**
  - *Chosen Approach*: Direct integration with `android.media.audiofx.Equalizer` and `android.media.audiofx.BassBoost` attached to the MediaPlayer `audioSessionId`.
  - *Why*: Delivers authentic hardware-accelerated sound enhancement with zero latency and low battery consumption.
- **Decision 3: Multi-Language Romanization Engine**
  - *Chosen Approach*: On-device compound transliterator with:
    1. Japanese Kanji/Katakana/Hiragana dictionary and compound word segmentation converting to Hepburn Romaji.
    2. Mandarin Hanyu Pinyin converter with tone marks/plain Latin options.
    3. Korean Hangul jamo decomposition to Revised Romanization.
  - *Why*: Zero network latency, privacy-preserving, works 100% offline during playback, and perfectly preserves line-by-line LRC timestamp synchronization.
- **Decision 4: Embedded Art & Cover Retrieval**
  - *Chosen Approach*: `MediaMetadataRetriever` for embedded ID3/MP4 album art + memory & disk LRU caching + online fallback search.
  - *Why*: Guarantees instant cover art rendering without blank frames, matching user expectation.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI Layer                        │
│                                                                        │
│  ┌───────────────────────┐  ┌───────────────────────────────────────┐  │
│  │ Classic Top Tabs      │  │ MiniLyrics Synchronized Screen        │  │
│  │ Songs|Artists|Albums  │  │ Album Art | Lyrics Canvas (Dual/Rom) │  │
│  │ Playlists|Playback    │  │ Timing Sync (+/-) | Scrub & Controls  │  │
│  └───────────┬───────────┘  └───────────────────┬───────────────────┘  │
│              │                                  │                      │
│              ▼                                  ▼                      │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ MiniLyrics Main ViewModel & UI State Machine                     │  │
│  │ - Current Track, Queue, Playback State, Position Flow            │  │
│  │ - Active Lyrics & Dual-Lyrics Cache, Theme & Effect State        │  │
│  └───────────┬──────────────────────────────────┬───────────────────┘  │
└──────────────┼──────────────────────────────────┼──────────────────────┘
               ▼                                  ▼
┌───────────────────────────────┐  ┌─────────────────────────────────────┐
│ Core Audio Engine             │  │ Lyrics & Romanization Pipeline      │
│ - MediaPlaybackService (FG)   │  │ - LRCLIB API & Netease API          │
│ - MediaPlayer + AudioSession  │  │ - Kanji->Romaji Morphological Engine│
│ - Equalizer & BassBoost (FX)  │  │ - Pinyin & Hangul Transliterator    │
│ - MediaSession & Notification │  │ - Title Sanitizer & Matcher         │
└──────────────┬────────────────┘  └──────────────────┬──────────────────┘
               │                                      │
               ▼                                      ▼
┌────────────────────────────────────────────────────────────────────────┐
│ Persistence & Storage (Room DB & DataStore)                            │
│ - LyricsAssociationDao: song path -> lyrics file & offset sync         │
│ - TrackCorrectionDao: custom title / artist / album overrides          │
│ - PlaylistDao & Favorites: custom playlists & play counts              │
│ - Theme & Lyrics Appearance Preferences (DataStore)                    │
└────────────────────────────────────────────────────────────────────────┘
```

#### Core Data Entities
- `Track`: ID, URI, title, sanitized title, artist, album, duration, albumArtUri, embeddedArtBytes, trackNumber.
- `LyricsLine`: Timestamp in milliseconds, original text, romanized text (Romaji/Pinyin/Hangul).
- `LyricsAssociation`: Primary key `songFilePath`, `lyricsContent`, `lyricsSource`, `offsetMillis`, `modifiedTimestamp`.
- `EqualizerState`: Enabled, preset index, 5 band levels (mB), bass boost strength.
- `ThemeConfig`: Theme name, background gradient/color, active lyric color, inactive lyric color, accent color, card background.

---

### 5. Implementation Stages & Verification Plan

1. **Gradle Dependencies & Android 16 Manifest Config**:
   - Enable Coil, Room, Retrofit, Datastore, OkHttp, and Media libraries in `app/build.gradle.kts`.
   - Configure permissions in `AndroidManifest.xml`: `READ_MEDIA_AUDIO`, `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `WAKE_LOCK`, `INTERNET`.
   - Update `metadata.json` and `strings.xml` to **MiniLyrics**.
2. **Audio Playback Engine & Background Service**:
   - Build `MediaPlaybackService` with `MediaPlayer`, `MediaSessionCompat`, Audio Focus management, noisy audio receiver (headset disconnect pause), and rich notification controls.
   - Connect real `android.media.audiofx.Equalizer` and `BassBoost`.
3. **Lyrics Engine, Online Search & Romanization**:
   - Build LRC parser with timestamp extraction and line synchronizer.
   - Implement `RomajiConverter` supporting Japanese Kanji, Katakana, Hiragana (Hepburn), Chinese Pinyin, and Korean Hangul.
   - Implement online lyrics repository fetching from **LRCLIB** and **Netease** with metadata cleaning and error resilience.
4. **MiniLyrics User Interface & 12 Themes**:
   - Implement classic top tab navigation (Songs, Artists, Albums, Playlists, Playback).
   - Build synchronized auto-scrolling lyrics view with tap-to-seek and timing calibration (`-0.5s`, `+0.5s`).
   - Implement Album Art loader with fallback vinyl art from the user's attachment.
   - Build 12 distinct theme color systems with dynamic switching.
5. **Title Correction & Lyrics Matcher Dialog**:
   - Create metadata editor modal allowing immediate Title/Artist edit and one-click online lyrics reassignment.
   - Persist song-to-lyrics associations in Room DB.
6. **Full Verification**:
   - Run `compile_applet` to confirm zero compilation errors.
   - Test playback lifecycle, background audio, lyrics synchronization, Romaji conversion, and equalizer functions.
