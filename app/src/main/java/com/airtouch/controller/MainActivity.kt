package com.airtouch.controller

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.airtouch.controller.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** Dashboard: live state, permissions, controls and navigation. */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val previewHandler = Handler(Looper.getMainLooper())

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { refreshStatuses() }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { refreshStatuses() }

    private val previewTick = object : Runnable {
        override fun run() {
            val bitmap = ControllerBus.previewBitmap
            if (ControllerBus.running && bitmap != null) {
                binding.ivPreview.setImageBitmap(bitmap)
                binding.tvPreviewPlaceholder.visibility = View.GONE
            } else if (!ControllerBus.running) {
                binding.ivPreview.setImageDrawable(null)
                binding.tvPreviewPlaceholder.visibility = View.VISIBLE
            }
            previewHandler.postDelayed(this, PREVIEW_REFRESH_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStart.setOnClickListener { startController() }
        binding.btnStop.setOnClickListener { stopController() }
        binding.btnEmergency.setOnClickListener { stopController() }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.btnCalibration.setOnClickListener {
            startActivity(Intent(this, CalibrationActivity::class.java))
        }
        binding.btnTutorial.setOnClickListener {
            startActivity(Intent(this, TutorialActivity::class.java))
        }

        binding.btnCameraGrant.setOnClickListener { requestCamera() }
        binding.btnOverlayGrant.setOnClickListener { requestOverlay() }
        binding.btnAccessibilityOpen.setOnClickListener { openAccessibilitySettings() }
        binding.btnNotifGrant.setOnClickListener { requestNotifications() }

        observeControllerState()
    }

    override fun onResume() {
        super.onResume()
        refreshStatuses()
        previewHandler.post(previewTick)
    }

    override fun onPause() {
        super.onPause()
        previewHandler.removeCallbacks(previewTick)
    }

    private fun observeControllerState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    ControllerBus.state.collect { state ->
                        binding.tvState.text = stateLabel(state)
                        binding.btnStart.isEnabled = state == ControllerState.STOPPED
                        binding.btnStop.isEnabled = state != ControllerState.STOPPED
                    }
                }
                launch {
                    ControllerBus.gesture.collect { gesture ->
                        binding.tvGesture.text = gestureLabel(gesture)
                    }
                }
            }
        }
    }

    // ---- controls --------------------------------------------------------

    private fun startController() {
        if (!hasCameraPermission()) {
            requestCamera()
            Toast.makeText(this, R.string.toast_camera_required, Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, AirTouchService::class.java)
        ContextCompat.startForegroundService(this, intent)
        Toast.makeText(this, R.string.toast_started, Toast.LENGTH_SHORT).show()
    }

    private fun stopController() {
        stopService(Intent(this, AirTouchService::class.java))
        Toast.makeText(this, R.string.toast_stopped, Toast.LENGTH_SHORT).show()
    }

    // ---- permissions -----------------------------------------------------

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    private fun requestCamera() {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            refreshStatuses()
        }
    }

    private fun requestOverlay() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun refreshStatuses() {
        // Camera
        val cameraOk = hasCameraPermission()
        setStatus(binding.tvCameraStatus, cameraOk)
        binding.btnCameraGrant.visibility = if (cameraOk) View.GONE else View.VISIBLE

        // Overlay
        val overlayOk = Settings.canDrawOverlays(this)
        setStatus(binding.tvOverlayStatus, overlayOk)
        binding.btnOverlayGrant.visibility = if (overlayOk) View.GONE else View.VISIBLE

        // Accessibility
        val accessibilityOk = isAccessibilityServiceEnabled()
        binding.tvAccessibilityStatus.text = getString(
            if (accessibilityOk) R.string.status_enabled else R.string.status_disabled
        )
        binding.tvAccessibilityStatus.setTextColor(
            ContextCompat.getColor(
                this,
                if (accessibilityOk) R.color.state_ok else R.color.state_err
            )
        )
        binding.btnAccessibilityOpen.visibility = if (accessibilityOk) View.GONE else View.VISIBLE

        // Notifications (only required on Android 13+)
        val notificationsOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        setStatus(binding.tvNotifStatus, notificationsOk)
        binding.btnNotifGrant.visibility = if (notificationsOk) View.GONE else View.VISIBLE
    }

    private fun setStatus(view: android.widget.TextView, granted: Boolean) {
        view.text = getString(if (granted) R.string.status_granted else R.string.status_denied)
        view.setTextColor(
            ContextCompat.getColor(this, if (granted) R.color.state_ok else R.color.state_err)
        )
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        if (ControlAccessibilityService.isConnected()) return true
        val expected = "$packageName/${ControlAccessibilityService::class.java.name}"
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    // ---- label helpers ---------------------------------------------------

    private fun stateLabel(state: ControllerState): String = getString(
        when (state) {
            ControllerState.STOPPED -> R.string.state_stopped
            ControllerState.INITIALIZING_CAMERA -> R.string.state_initializing
            ControllerState.POINTER -> R.string.state_pointer
            ControllerState.SCROLL -> R.string.state_scroll
            ControllerState.PINCH_PRESSED -> R.string.state_pinch
            ControllerState.PAUSED -> R.string.state_paused
            ControllerState.TRACKING_LOST -> R.string.state_tracking_lost
            ControllerState.ERROR -> R.string.state_error
        }
    )

    private fun gestureLabel(gesture: GestureType): String = getString(
        when (gesture) {
            GestureType.NONE -> R.string.gesture_none
            GestureType.OPEN_PALM -> R.string.gesture_open_palm
            GestureType.INDEX_POINT -> R.string.gesture_index_point
            GestureType.PINCH -> R.string.gesture_pinch
            GestureType.FIST -> R.string.gesture_fist
        }
    )

    companion object {
        private const val PREVIEW_REFRESH_MS = 120L
    }
}
