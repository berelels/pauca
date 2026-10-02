# Store images

These scripts produce the Play Store screenshots and feature graphic from real captures of the app, in English, Portuguese and Spanish.

| File | What it does |
|---|---|
| `prefs.py` | Writes Pauca's settings and sample home screen straight into the emulator, so each capture shows one look |
| `demo.sh` | Puts the status bar in demo mode: 9:42, full battery, no notifications |
| `capture_all.sh <en\|pt\|es>` | Switches the emulator's language and captures every screen the slides use into `shots/<lang>/` |
| `slides.html` | The slide layouts and their text in the three languages |
| `render.js` | Renders `slides.html` to PNG with Electron |
| `render.sh` | Renders every language into `fastlane/metadata/android/<locale>/images/` and rebuilds `art/screens.png` |
| `wallpaper.jpg` | The sample image used for the Wallpaper theme |

## Capturing

You need an emulator with a `google_apis` image (it has to allow `adb root`) and the debug build installed:

```bash
adb -s emulator-5554 root
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell cmd role add-role-holder --user 0 android.app.role.HOME app.pauca.debug
adb -s emulator-5554 shell settings put global auto_time 0
art/store/capture_all.sh en
```

Run `capture_all.sh` again with `pt` and `es`. Every language uses the 24-hour clock, so the time reads 09:42.

## Rendering

```bash
ELECTRON=/path/to/electron art/store/render.sh
```

Without `ELECTRON`, the script falls back to `npx electron`. Editing a headline only needs this step; the captures stay as they are.
