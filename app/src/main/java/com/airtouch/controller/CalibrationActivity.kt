package com.airtouch.controller

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.airtouch.controller.databinding.ActivityCalibrationBinding
import java.util.concurrent.atomic.AtomicReference

/**
 * Optional calibration flow. Runs its own camera session (so AirTouch must be
 * stopped first) and records the user's neutral palm centre.
 */
class CalibrationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalibrationBinding
    private lateinit var calibration: CalibrationManager
    private var cameraSession: CameraSession? = null
    private var handTracking: HandTracking? = null

    private val latestPalm = AtomicReference<Landmark?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalibrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        calibration = CalibrationManager(this)
        binding.tvCalibStatus.text = getString(
            if (calibration.isCalibrated) R.string.calibration_saved
            else R.string.calibration_need_hand
        )

        binding.btnCapture.setOnClickListener { capture() }
        binding.btnCalibReset.setOnClickListener {
            calibration.reset()
            binding.tvCalibStatus.text = getString(R.string.calibration_need_hand)
            Toast.makeText(this, R.string.calibration_reset, Toast.LENGTH_SHORT).show()
        }
        binding.btnCalibDone.setOnClickListener { finish() }

        if (ControllerBus.running) {
            Toast.makeText(this, R.string.toast_stop_first, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        startCamera()
    }

    private fun startCamera() {
        val session = CameraSession(this, this)
        cameraSession = session

        val config = SettingsRepository(this).load()
        val tracking = HandTracking(this, config) { hand, _ ->
            latestPalm.set(hand?.palmCentre())
        }
        handTracking = tracking

        val analyzer = CameraFrameAnalyzer(tracking) { bitmap ->
            runOnUiThread { binding.ivCalibPreview.setImageBitmap(bitmap) }
        }

        session.start(analyzer) {
            runOnUiThread {
                Toast.makeText(this, R.string.calibration_camera_error, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun capture() {
        val centre = latestPalm.get()
        if (centre == null) {
            binding.tvCalibStatus.text = getString(R.string.calibration_need_hand)
            Toast.makeText(this, R.string.calibration_need_hand, Toast.LENGTH_SHORT).show()
            return
        }
        calibration.save(centre)
        binding.tvCalibStatus.text = getString(R.string.calibration_saved)
        Toast.makeText(this, R.string.calibration_saved, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        cameraSession?.stop()
        handTracking?.close()
        handTracking = null
        super.onDestroy()
    }
}
