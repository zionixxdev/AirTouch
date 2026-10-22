package com.airtouch.controller

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.airtouch.controller.databinding.ActivityTutorialBinding

/** Static help: how to perform each gesture, grant permissions and troubleshoot. */
class TutorialActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTutorialBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTutorialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvTutorial.text = TUTORIAL
        binding.btnOpenAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    companion object {
        private val TUTORIAL = """
            GESTURES

            • Open palm — pointer mode. Hold your hand open, fingers spread, palm
              towards the front camera. Move your hand to move the floating cursor.
              Keep your hand steady to avoid jitter; the cursor freezes if tracking
              is lost and no tap is ever generated just by hovering.

            • Index finger — scroll mode. Fold the other fingers and raise only your
              index finger. Move it up to scroll the screen down, and down to scroll
              the screen up. Small tremor movements are ignored. While scrolling, the
              cursor pauses until you deliberately switch back to an open palm.

            • Pinch — click. Touch your thumb and index fingertip together. One tap is
              fired when you release the pinch, at the cursor position. Keep the pinch
              held and no repeated clicks occur; you must release and pinch again.

            • Closed fist — emergency pause. All actions stop immediately and the
              cursor turns grey. Show an open palm or an index finger to resume.

            • No hand — the cursor freezes and a "tracking lost" state is shown. Nothing
              is clicked or scrolled until a stable hand returns.

            PERMISSIONS

            1. Camera — required to see your hand. Grant it from the dashboard.
            2. Overlay ("Display over other apps") — required for the floating cursor.
            3. Accessibility — required so AirTouch can perform taps and scrolls.
               Open Settings → Accessibility → AirTouch Gesture Control and enable it.
            4. Notifications (Android 13+) — for the ongoing "AirTouch is active"
               notification that keeps tracking alive.

            TROUBLESHOOTING

            • Hand not detected: improve lighting, keep your whole hand in frame and
              30–60 cm from the camera, and lower "Hand detection confidence" in Settings.
            • Cursor jumps: increase "Pointer smoothing" and "Pointer dead zone", or
              run Calibration to re-centre the pointer.
            • Accidental clicks: raise the "Pinch detection threshold" and "Click
              cooldown", or disable gestures you do not use.
            • Scrolling too fast: lower "Scroll sensitivity" and raise "Scroll dead zone".
            • Taps do nothing on some screens: protected system screens, secure windows,
              permission dialogs and some games refuse simulated input — this is an
              Android limitation.
            • Nothing works after backgrounding: bring AirTouch back to the foreground
              and start it again; Android may stop camera access in the background.

            PRIVACY

            All camera frames and hand landmarks are processed on-device with MediaPipe.
            AirTouch never uploads frames, never adds analytics, and needs no API keys.
        """.trimIndent()
    }
}
