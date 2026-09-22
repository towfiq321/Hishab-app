# Hishab Build Fix 2

Fixed the Kotlin compilation error in `MoreScreen.kt`:

`Unresolved reference: SegmentedButtonScope`

The segmented-button API uses `SingleChoiceSegmentedButtonRowScope` for `SingleChoiceSegmentedButtonRow`, so `ThemeOption` now uses that scope.

Build command:

`./gradlew :app:assembleDebug`

Expected APK:

`app/build/outputs/apk/debug/app-debug.apk`
