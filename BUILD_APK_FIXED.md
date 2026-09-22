# Hishab APK build fix

The original GitHub Actions workflow failed because it configured Java and Gradle but did not explicitly provision the Android SDK packages required by the Android Gradle Plugin.

This package contains a corrected workflow that:
- Uses JDK 17.
- Installs Android platform 34 and build-tools 34.0.0.
- Uses Gradle 8.9, matching the project's pinned AGP 8.5.2 configuration.
- Builds `:app:assembleDebug`.
- Verifies `app-debug.apk` exists.
- Uploads the APK as a GitHub Actions artifact named `hishab-debug-apk`.

After pushing this project to GitHub, open **Actions → Build Hishab APK**. The workflow can be run manually, and it also runs on pushes to `main`.
