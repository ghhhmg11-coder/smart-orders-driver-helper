# Smart Orders Driver Helper

A private Jeeny Driver auto-accept helper app for Android.

## Features

- **Auto-Accept**: Automatically clicks the "قبول العرض" button in Jeeny Driver based on your rules
- **Floating Overlay**: Movable floating button always above Jeeny — START/STOP from anywhere
- **Rules Engine**: Filter by price, pickup minutes, and pickup distance
- **Debug Log**: Full in-app log viewer — no ADB needed
- **Local Login**: Username `admin`, Password `1234`

## Setup Steps (on Android)

1. **Install the APK** — download from GitHub Actions artifacts
2. **Login** — use `admin` / `1234`
3. **Grant Accessibility Permission** → Settings → Accessibility → Smart Orders → Enable
4. **Grant Overlay Permission** → Settings → "Display Over Other Apps" → Smart Orders → Allow
5. **Set your rules** in the Rules screen
6. **Enable Auto Accept** on the Dashboard OR tap the floating button → START

## How to Get the APK

### Via GitHub Actions (recommended):
1. Push this project to a GitHub repository
2. GitHub Actions will automatically build the APK on every push
3. Go to **Actions** tab → select the latest run → **Artifacts** → download `smart-orders-driver-helper-debug`
4. Extract the ZIP — inside is `app-debug.apk`
5. Transfer to your phone and install (enable "Unknown Sources" in settings)

### Trigger manually:
1. Go to **Actions** tab in your GitHub repo
2. Click **"Build Debug APK"**
3. Click **"Run workflow"**
4. Wait ~3-5 minutes
5. Download from **Artifacts**

## Accessibility Service Details

- Monitors **all packages** (no packageNames filter in XML)
- Filters Jeeny in code: `com.jeeny.driver`, `com.jeeny.drivers`, or any package containing "jeeny"
- Listens to: `WINDOW_STATE_CHANGED`, `WINDOW_CONTENT_CHANGED`, `WINDOWS_CHANGED`, `VIEW_CLICKED`, `VIEW_FOCUSED`
- Reads text from **all accessibility windows**, not just rootInActiveWindow
- Extracts: price, pickup minutes, pickup distance, raw screen text

## Click Strategy

1. **Primary**: Find node with text "قبول العرض" → click node → walk up 5 parent levels if not clickable
2. **Fallback**: GestureDescription tap at center-bottom of screen (88% height)

## Package Info

- Package: `com.smartorders.driverhelper`
- Min SDK: 26 (Android 8.0)
- Language: Kotlin + Jetpack Compose
- Target: Jeeny Driver app

## Project Structure

```
app/src/main/java/com/smartorders/driverhelper/
├── LoginActivity.kt              # Login screen
├── MainActivity.kt               # Main nav + bottom bar
├── AppState.kt                   # Global state (flows)
├── data/
│   ├── AppPreferences.kt         # SharedPreferences wrapper
│   └── TripInfo.kt               # Trip parsing from screen text
├── service/
│   ├── DriverHelperAccessibilityService.kt   # Core auto-click engine
│   └── FloatingOverlayService.kt              # Floating button/panel
└── ui/
    ├── Theme.kt                  # Dark purple theme
    └── screens/
        ├── DashboardScreen.kt    # Stats + ON/OFF toggle
        ├── RulesScreen.kt        # Price/minutes/distance rules
        ├── DebugLogScreen.kt     # Live debug log viewer
        └── SettingsScreen.kt     # Permissions + reset + logout
```
