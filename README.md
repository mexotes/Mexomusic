# Mexo Music — Android starter project

A Kotlin + Jetpack Compose Jamendo music discovery player.

## Features
- Search Jamendo tracks
- Album art, artist and duration
- Stream tracks using AndroidX Media3 ExoPlayer
- Local favorites playlist
- Mini-player with play/pause and next
- GitHub Actions debug APK build

## Add your own Jamendo Client ID

1. Create an app in the Jamendo Developer Portal: https://devportal.jamendo.com/
2. Copy its `client_id`.
3. In GitHub, open your repository → Settings → Secrets and variables → Actions → New repository secret.
4. Name it exactly `JAMENDO_CLIENT_ID`, paste the Client ID as its value.
5. Run Actions → Build Mexo Music APK → Run workflow.
6. Download the `Mexo-Music-debug-apk` artifact and extract `app-debug.apk`.

For local builds, add this line to the root `local.properties` file:
`JAMENDO_CLIENT_ID=your_client_id`

The app has a documented Jamendo test Client ID fallback (`709fa152`) for quick read-API testing. Replace it with your own before publishing.

## Build locally
Requires JDK 17, Android SDK 35, and Gradle 8.9:
`gradle :app:assembleDebug`

## Important licensing note
Jamendo track streaming and reuse are subject to Jamendo API terms and each track's license. Do not assume every track can be downloaded or reused commercially. This starter app streams the `audio` URL and does not implement downloads. Check the license/permissions for each track and comply with attribution and other license conditions.

## Notes
- This is a starter version, not a Play Store release build.
- The Client ID is visible in a decompiled APK. Do not put a client secret in the app.
- For a public production app, consider routing API calls through your own backend and review Jamendo's current terms.
