# MSPS Android project

This repository contains the MSPS website bundled inside an Android WebView application.

## Included

- `index.html` — root copy of the complete MSPS website source.
- `app/src/main/assets/index.html` — the same website loaded by the APK.
- Android WebView wrapper with JavaScript, DOM storage, file chooser for backup restore, and internet access for the existing Supabase backend.
- Android native print bridge (`AMUAndroidPrint.print()`) that opens Android's system print preview.
- Android native backup bridge (`AMUAndroidPrint.saveBackup(...)`) that saves JSON backups to Downloads.
- Back-button handling that closes the topmost MSPS popup/panel before leaving the app.
- Status/navigation bars use the dark MSPS theme; app content is laid out below the Android status bar.
- GitHub Actions workflow builds a debug APK and uploads it as an artifact.

## Upload to GitHub

Upload the **contents of this folder** to the root of your new repository. Do not upload the `msps_new_repo` folder as an extra nested folder. The repository root must show `app/`, `.github/`, `build.gradle`, `settings.gradle`, `gradle.properties`, `README.md`, and `index.html`.

## Build

After uploading, open **Actions → Build MSPS Android APK**. The workflow also runs automatically after a push to `main`. Download the `MSPS-Android-APK` artifact from the successful run; it contains `app-debug.apk`.

## Important backend note

The website is bundled in the APK, but its existing Supabase project and Edge Functions remain online services. The device needs an internet connection for login, live data, transactions, and email/backup status. No Supabase service-role key is included in the Android app.

## Before release

This is a debug-build starter project. Verify the actual APK on the target Android phone, especially login, nested popup Back behavior, report printing, backup download, and backup restore. A release-signed APK/AAB requires a signing key and secure GitHub secrets, which are not included here.
