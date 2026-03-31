# MasterPaper

Android Live Wallpaper App with Jetpack Compose.

## Features

- Browse wallpapers from GitHub repository
- Live wallpapers support
- Ringtone downloads
- Categories browsing
- Preview and set wallpapers
- Dark mode support

## Requirements

- Android SDK 35
- Gradle 9.3.1
- Kotlin 1.9.24

## Setup

1. Clone the repository
2. Create `local.properties` with your GitHub token:
   ```
   GITHUB_TOKEN=your_github_token_here
   ```
3. Build the project:
   ```bash
   ./gradlew assembleDebug
   ```

## Configuration

Add the following to `local.properties`:
- `GITHUB_TOKEN` - GitHub Personal Access Token for API access
- `KEYSTORE_FILE` - Path to keystore (optional)
- `KEYSTORE_PASSWORD` - Keystore password (optional)
- `KEY_ALIAS` - Key alias (optional)
- `KEY_PASSWORD` - Key password (optional)

## Tech Stack

- Kotlin
- Jetpack Compose
- MVVM Architecture
- Retrofit + OkHttp
- Coil for images
- ExoPlayer for video
- DataStore Preferences
- Navigation Compose

## License

MIT
