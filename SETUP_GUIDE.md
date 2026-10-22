# AirTouch — phone-only setup guide

Everything here is done from an Android phone's browser. You never run Gradle
commands on your phone.

---

## 1. Create the repository

1. Open GitHub in your phone browser and sign in.
2. Tap **+ → New repository**.
3. Name it **`AirTouch`**, set it **Public** (public repos get free Actions
   minutes), and create it. Do **not** add a README (you already have one).

## 2. Upload the project files

Upload the whole project, keeping the folder structure exactly as delivered:

```
AirTouch/
├── .github/workflows/android.yml
├── gradle/libs.versions.toml
├── gradle/wrapper/gradle-wrapper.jar
├── gradle/wrapper/gradle-wrapper.properties
├── gradlew
├── gradlew.bat
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── .gitignore
├── README.md
├── SETUP_GUIDE.md
├── TESTING_CHECKLIST.md
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── assets/README.txt
        ├── java/com/airtouch/controller/*.kt
        └── res/...
```

**Easiest method (desktop-style):** on the repo page tap **Add file → Upload
files**, then drag the extracted folder contents in. The web uploader preserves
the folder paths of files you drag.

**If the phone browser struggles with the binary `gradle-wrapper.jar`:** you can
skip it — the CI workflow regenerates the wrapper automatically before building.
Everything else must be uploaded.

> Important: `.github/`, `gradle/` and `app/` must keep their exact names, and
> `.github/workflows/android.yml` must sit at that exact path or Actions will not
> find the workflow.

## 3. The MediaPipe model

You do **not** upload the model. The workflow downloads
`hand_landmarker.task` and verifies its SHA-256
(`fbc2a30080c3c557093b5ddfc334698132eb341044ccee322ccf8bcf3607cde1`) before
building. If you prefer to commit it yourself, place it at
`app/src/main/assets/hand_landmarker.task`.

## 4. Enable Actions if prompted

Open the **Actions** tab. If GitHub asks you to enable workflows, tap the button
to do so.

## 5. Run the build

1. **Actions → Android CI → Run workflow → Run workflow** (branch `main`).
2. Alternatively, any push to `main` triggers it automatically.

## 6. Read the build logs

Tap the running job, then the **build** job, and expand steps. The
**Build debug APK** step shows Gradle output. If it fails, expand it and copy the
error lines.

## 7. Download the APK

1. Open the finished run.
2. Scroll to **Artifacts** and download **`AirTouch-debug-apk`**.
3. It downloads as a `.zip` — extract it with any file manager to get
   `app-debug.apk`.

## 8. Install on Android 12

1. Open the extracted `app-debug.apk`.
2. Allow **Install unknown apps** for your browser/file manager when prompted.
3. Install, then open AirTouch.

## 9. Grant permissions and enable the service

1. On the dashboard tap **Grant** next to **Camera permission**.
2. Tap **Grant** next to **Overlay permission** → enable *Allow display over
   other apps*.
3. Tap **Open** next to **Accessibility service** → **AirTouch Gesture Control**
   → turn it **On** and confirm.
4. On Android 13+, tap **Grant** next to **Notification permission**.

## 10. Run and test

1. Tap **Start AirTouch**. A cursor appears and a notification shows AirTouch is
   active.
2. Try each gesture (see `TESTING_CHECKLIST.md`).
3. Tap **Stop AirTouch**, or **Emergency Stop**, to end everything at once. The
   notification's *Emergency Stop* action works too.

## 11. Optional calibration

1. **Stop AirTouch** first (the camera is shared).
2. Open **Calibration**, hold an open palm steady, tap **Capture neutral
   position**, then **Done**.

## 12. Reporting build errors

If the build fails, send back the exact error text from the failed step. Fixes
are given as complete file replacements so you can re-upload just that file.
