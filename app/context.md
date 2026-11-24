# Aether Ultra - Android Video Player Application

## Overview
Aether Ultra is a premium, sophisticated dark-themed Android application tailored for playing and organizing educational or local video content, specifically structured around NEET PG medical subjects. The app is built completely in modern Android using Kotlin, Jetpack Compose, Media3 (ExoPlayer), Room Database, and Material Design 3.

## Visual Identity & Theme
- **Theme**: "Sophisticated Dark" (Aether Primary `D4AF37` / Gold, with dark gray/black backgrounds like `0F0F0F` and `161616`).
- **Typography & Aesthetics**: Sleek, uppercase tracking for labels ("MEMBER ACCESS"), clean typography, rounded cards (up to 32dp), semi-transparent gradients, and subtle shadows to produce a high-end cinematic feel.
- **Edge-to-Edge**: Translucent or transparent status bar and navigation bar padding.

## Core Features
1. **Local Video Import**
   - Users can securely import local video files using the standard Android Storage Access Framework (SAF).
   - Manages runtime permissions for external storage seamlessly across older and newer Android API levels (`READ_EXTERNAL_STORAGE`, `READ_MEDIA_VIDEO`, `MANAGE_EXTERNAL_STORAGE`).
   - Maintains persistent URI permissions to allow videos to load offline indefinitely.

2. **Subject Mapped Library Engine**
   - **Categorization**: Integrates natively with an array of 20 predefined medical subjects (Anatomy, Physiology, Biochemistry, Pathology, etc.).
   - **Adding Content**: Whenever a video is added, the action triggers a bottom sheet prompting the user to classify the video into a specific subject basket.
   - **Persistence**: All library structures are handled offline-first, cached into an internal `Room` SQL database (`videos` table storing ID, URI, Title, associated Subject, and timestamp).

3. **Home Dashboard**
   - **Hero Video Card**: Showcases the most recently added video prominently with a 16:9 cinematic display, gradient overlays, and a "Resume Playing" call-out.
   - **Recently Added Grid**: Displays secondary, older items in a 4:3 grid with high contrast metadata beneath the hero view.
   - **Bottom Navigation**: Sticky M3 bottom bar housing Home, Library, and Engine routes.

4. **Library & Drill-down Navigation**
   - **Library Overview**: Neumorphic, squircle grid tiles for primary buckets: Subjects, Playlists, History, Bookmarks.
   - **Subject Explorer**: Expanding into "Subjects" renders an alphabetically navigable list of the standard NEET PG curriculum.
   - **Subject Detail**: Selecting a subject displays an isolated grid of solely the videos tagged identically. The FAB allows instantly injecting a new video into this exact category.

5. **Advanced Custom Video Player (ExoPlayer)**
   - **Engine**: Powered by AndroidX Media3 `ExoPlayer` wrapped within a borderless Compose boundary.
   - **Lifecycle Aware**: Playback accurately pauses/resumes responding gracefully to Android Lifecycle (`LifecycleEventObserver`).
   - **Custom Overlay**: 
     - Hidden native Media3 controls. Rebuilt completely in Compose.
     - Auto-hides after 3 seconds of inactivity.
     - Center animated Play/Pause, Rewind, and Fast-Forward capabilities.
     - Precision scrubber slider synced precisely to ExoPlayer duration polling.
   - **Pro Gestures**: Supports Double-Tap on Left/Right edges of the screen to seek 10-seconds backward or forward respectively.
   - **Advanced Features Settings Menu**:
     - **Speed Controls**: Cycles between progressive playback speeds: 1.0x, 1.5x, and 2.0x.
     - **Picture-in-Picture (PiP)**: Full support for native background floating PiP mode allowing multitasking during playback.

## Architecture
- **Language**: Kotlin.
- **UI Framework**: Jetpack Compose Navigation for managing state between screens.
- **Data Layer**: Room Database (SQLite) + Coroutine Flow (Reactive StateFlow).
- **Dependency & State Injection**: `VideoViewModel` utilizing `viewModelScope` lifecycle bounds logic cleanly decoupled from views.
- **Image/Thumbnail Loading**: Coil library integrated to render native hardware-accelerated video frame thumbnails efficiently.
