# AirTouch — AI Gesture Controller for Android

Control an Android phone with hand gestures seen by the **front camera**. An open
palm moves a floating cursor, an index finger scrolls, a thumb–index pinch clicks,
and a closed fist pauses everything. All hand tracking runs **on-device** with
MediaPipe — no internet, no API keys, no cloud, no analytics.

This repository builds a debug APK entirely in the cloud through **GitHub
Actions**, so it can be developed and compiled from a phone with no local
Android SDK, no Android Studio and no Gradle install.

---

## What it does

| Gesture | Mode | Behaviour |
|---|---|---|
| Open palm | **Pointer** | Moves the floating cursor with the palm centre |
| Index finger up / down | **Scroll** | Finger up scrolls content down, finger down scrolls content up |
| Thumb + index pinch | **Click** | One tap per pinch cycle, at the cursor position |
| Closed fist | **Pause** | Stops pointer, scroll and click immediately |
| No hand | **Tracking lost** | Cursor freezes; no taps or scrolls are generated |

The gesture logic lives in an explicit state machine
(`STOPPED → INITIALIZING_CAMERA → POINTER ⇄ SCROLL → PINCH_PRESSED / PAUSED /
TRACKING_LOST / ERROR`) with temporal debouncing, hysteresis and cooldowns so
noisy frames cannot cause accidental input.

---

## Architecture

```
Camera (CameraX, front)                 Overlay (WindowManager)
        │ ImageAnalysis                        ▲
        ▼                                       │ cursor position
 CameraFrameAnalyzer ──bitmap──► HandTracking   │
        │  (throttle, rotate,      (MediaPipe   │
        │   back-pressure)          Hand        │
        ▼                           Landmarker) │
   GestureEngine ◄── HandLandmarks ─────────────┘
        │  GestureResult
        ▼
   AirTouchService ──► ControlAccessibilityService (dispatchGesture)
        │
        └──► ControllerBus (StateFlow) ──► MainActivity / UI
```

The engine has **no Android dependencies**, so it can be unit-tested on the JVM.
Camera, UI, gesture logic and accessibility are each isolated.

### Source files (`app/src/main/java/com/airtouch/controller/`)

| File | Responsibility |
|---|---|
| `MainActivity.kt` | Dashboard, permissions, start/stop, live state |
| `SettingsActivity.kt` | Sliders and switches, persisted locally |
| `CalibrationActivity.kt` | Optional neutral-position calibration |
| `TutorialActivity.kt` | Gesture help and troubleshooting |
| `AirTouchService.kt` | Foreground service owning the pipeline |
| `CameraSession.kt` | CameraX front-camera ImageAnalysis binding |
| `CameraFrameAnalyzer.kt` | Frame → upright bitmap → MediaPipe |
| `HandTracking.kt` | MediaPipe Hand Landmarker wrapper |
| `GestureEngine.kt` | State machine + gesture interpretation |
| `GestureState.kt` | States, gesture types, actions, results |
| `GestureConfig.kt` | All tunables |
| `HandLandmarks.kt` | Landmark holder + helpers |
| `ControlAccessibilityService.kt` | Taps and scrolls via `dispatchGesture` |
| `OverlayController.kt` | Floating cursor overlay |
| `SettingsRepository.kt` | SharedPreferences persistence |
| `CalibrationManager.kt` | Calibration storage |
| `ControllerBus.kt` | Shared observable state for the UI |

---

## Building the APK (no local tools required)

1. Push to `main`, or run **Actions → Android CI → Run workflow**.
2. Wait for the job to finish.
3. Download the **`AirTouch-debug-apk`** artifact and install
   `app-debug.apk` on your Android 12 device.

The workflow pins JDK 17, sets up the Android SDK, downloads the MediaPipe model
with a **checksum check**, builds `assembleDebug` and uploads the APK. See
[`SETUP_GUIDE.md`](SETUP_GUIDE.md) for the exact phone-only steps.

---

## Permissions and their purpose

| Permission | Why |
|---|---|
| `CAMERA` | See your hand |
| `SYSTEM_ALERT_WINDOW` | Draw the floating cursor |
| `BIND_ACCESSIBILITY_SERVICE` | Perform taps and scrolls |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_CAMERA` | Keep tracking alive with a visible notification |
| `POST_NOTIFICATIONS` | Show the ongoing notification (Android 13+) |

All four are granted by the user through standard Android settings. AirTouch
never enables anything silently.

---

## Honest limitations

- **Not a substitute for a full build.** This project was assembled and statically
  checked, but it has **not** been compiled in a real Android/Gradle environment
  from here, and the gestures have **not** been validated on a physical device.
  The GitHub Actions build is the first real compile — treat its output as
  authoritative and report any errors so they can be fixed with exact file
  changes.
- Simulated accessibility input cannot reach every surface: protected system
  screens, secure windows, permission dialogs and some games refuse it.
- Background camera access is subject to Android's foreground-service
  restrictions; tracking is designed to run while AirTouch is active.
- Detection quality depends on lighting and how well your hand fills the frame.

## Privacy

Camera frames and landmarks are processed entirely on-device. There is no
telemetry, no analytics and no network access at runtime.

## License / model attribution

`hand_landmarker.task` is Google's MediaPipe Hand Landmarker model
(`float16/1`), downloaded at build time from
`storage.googleapis.com/mediapipe-models/` and verified by SHA-256. MediaPipe is
licensed under Apache 2.0.
