# 📺 Android TV Browser — Auto-Refresh + Kiosk Mode

A lightweight WebView-based browser for Android TV with auto-refresh and full-screen kiosk mode.

---

## Features

- ✅ **URL Setup Screen** — configure on first launch
- ✅ **Auto-Refresh** — configurable intervals from 15s to 1 hour
- ✅ **Kiosk / Full-Screen Mode** — hides system UI, blocks back button
- ✅ **D-pad / Remote Navigation** — fully navigable with TV remote
- ✅ **Overlay Control Bar** — press Menu or Enter to show/hide controls
- ✅ **Live Countdown Timer** — shows next refresh time
- ✅ **Settings Dialog** — change refresh interval at runtime
- ✅ **URL Change Dialog** — change URL without leaving the app
- ✅ **JavaScript enabled** — supports modern web apps
- ✅ **Persists settings** — remembers URL + settings across reboots

---

## How to Use (Remote Controls)

| Remote Button | Action |
|---|---|
| **MENU** | Show/hide the top control bar |
| **ENTER / OK** | Show control bar (if hidden) |
| **BACK** | Dismiss control bar (Kiosk: does nothing) |
| **D-pad** | Navigate within web page |

---

## Build Instructions

### Prerequisites
- Android Studio (Hedgehog or newer)
- JDK 17
- Android SDK with API 34

### Steps

1. **Open the project** in Android Studio:
   - File → Open → select the `AndroidTVBrowser` folder

2. **Sync Gradle**:
   - Click "Sync Now" in the top banner

3. **Connect your Android TV** via ADB or use an emulator:
   ```bash
   adb connect <TV_IP_ADDRESS>:5555
   ```

4. **Run the app**:
   - Click the green ▶ Run button, or:
   ```bash
   ./gradlew installDebug
   ```

5. **Build APK** for sideloading:
   ```bash
   ./gradlew assembleDebug
   # Output: app/build/outputs/apk/debug/app-debug.apk
   ```

### Install APK on TV via ADB
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Configuration

All settings are stored in SharedPreferences and persist across app restarts.

- `KEY_URL` — the URL to load
- `KEY_REFRESH_INTERVAL` — refresh interval in seconds (0 = disabled)
- `KEY_KIOSK_MODE` — whether kiosk/fullscreen mode is enabled
- `KEY_FIRST_LAUNCH` — set to `true` to force re-showing the setup screen

To force re-configuration, press **Menu → Settings → Reset App**.

---

## Customization

- **Default URL**: Modify `SetupActivity.kt` line `urlInput.setText(prefs.getString(KEY_URL, "https://"))`
- **User Agent**: Modify `BrowserActivity.kt` `setupWebView()` — the UA string is set to look like Chrome on Android
- **Overlay timeout**: Change `OVERLAY_HIDE_DELAY` constant in `BrowserActivity.kt`

---

## Project Structure

```
app/src/main/
├── java/com/atvbrowser/
│   ├── SetupActivity.kt       # First-launch config screen
│   └── BrowserActivity.kt     # Main browser + auto-refresh logic
├── res/
│   ├── layout/
│   │   ├── activity_setup.xml
│   │   └── activity_browser.xml
│   ├── drawable/              # UI shape drawables
│   └── values/
│       ├── strings.xml
│       └── themes.xml
└── AndroidManifest.xml
```
