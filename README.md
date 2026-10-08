# Social Digest

Social Digest is an Android app that collects notifications from selected social and messaging apps and presents them in a dashboard. It highlights potentially important notifications and provides recent activity and notification views.

## Features

- Choose which supported apps to monitor.
- View recent notifications and activity summaries.
- Highlight notifications that may need attention.
- Mark individual notifications as very important or dismiss them.

## Requirements

- Android Studio with Android SDK 34.
- JDK 17.
- An Android device or emulator running Android 7.0 (API 24) or later.

## Build and run

1. Clone this repository and open its root directory in Android Studio.
2. Allow Gradle to sync and download the required dependencies.
3. Run the `app` configuration on a connected device or emulator.
4. Follow the in-app setup and grant notification access in Android settings when prompted.

You can also build a debug APK from the project root with the Gradle wrapper:

```powershell
.\gradlew.bat assembleDebug
```

## Privacy

Android notification access is required for the app to read notifications from the apps you select. Notification details are stored locally by the app to display its dashboard and activity views. Review the app's behavior and your device's backup settings before using it with sensitive notifications.

## Project structure

- `app/` — Android application source, resources, and Gradle configuration.
- `gradle/` — Gradle wrapper files.
- `extras/` — Additional project notes.

Local Android SDK configuration (`local.properties`), build output, IDE metadata, environment files, and common signing-key files are excluded from version control. Keep credentials and signing material out of the repository.
