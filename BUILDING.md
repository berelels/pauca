# Building Pauca

Requirements: JDK 17+ and the Android SDK (platform 36).

```bash
./gradlew assembleFullDebug
```

The APK ends up in `app/build/outputs/apk/full/debug/app-full-debug.apk`. To install it on a phone with USB debugging on:

```bash
adb install -r app/build/outputs/apk/full/debug/app-full-debug.apk
```

`assembleLiteDebug` builds Pauca Lite instead (`app/build/outputs/apk/lite/debug/`). Both come from the same code: the `lite` flavor turns off what is only in the full version (see `helper/Edition.kt`).

Then choose Pauca as your home app (Settings › Apps › Default apps › Home app).

## Grayscale focus mode

Android only lets an app change the screen colors with a special permission. To grant it, run this once (in the debug build the package is `app.pauca.debug`):

```bash
adb shell pm grant app.pauca android.permission.WRITE_SECURE_SETTINGS
```
