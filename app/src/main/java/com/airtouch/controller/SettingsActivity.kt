package com.airtouch.controller

import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.airtouch.controller.databinding.ActivitySettingsBinding
import java.util.Locale

/** Settings screen. Every change is written straight to [SettingsRepository]. */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var repo: SettingsRepository
    private lateinit var config: GestureConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = SettingsRepository(this)
        config = repo.load()

        bindSliders()
        bindSwitches()

        binding.btnResetDefaults.setOnClickListener {
            repo.resetToDefaults()
            config = repo.load()
            recreate()
        }
    }

    private fun persist(transform: (GestureConfig) -> GestureConfig) {
        config = transform(config)
        repo.save(config)
    }

    private fun bindSliders() {
        bindSlider(
            binding.seekPointerSensitivity, binding.valuePointerSensitivity,
            0.5f, 4.0f, { format(Locale.US, "%.2f\u00d7", it) }, config.pointerSensitivity
        ) { v -> persist { it.copy(pointerSensitivity = v) } }

        bindSlider(
            binding.seekPointerSmoothing, binding.valuePointerSmoothing,
            0.05f, 1.0f, { format(Locale.US, "%.2f", it) }, config.pointerSmoothing
        ) { v -> persist { it.copy(pointerSmoothing = v) } }

        bindSlider(
            binding.seekPointerDeadzone, binding.valuePointerDeadzone,
            0f, 20f, { format(Locale.US, "%.0f px", it) }, config.pointerDeadZonePx
        ) { v -> persist { it.copy(pointerDeadZonePx = v) } }

        bindSlider(
            binding.seekScrollSensitivity, binding.valueScrollSensitivity,
            0.2f, 3.0f, { format(Locale.US, "%.2f\u00d7", it) }, config.scrollSensitivity
        ) { v -> persist { it.copy(scrollSensitivity = v) } }

        bindSlider(
            binding.seekScrollDeadzone, binding.valueScrollDeadzone,
            0.01f, 0.15f, { format(Locale.US, "%.3f", it) }, config.scrollDeadZone
        ) { v -> persist { it.copy(scrollDeadZone = v) } }

        bindSlider(
            binding.seekPinchThreshold, binding.valuePinchThreshold,
            0.2f, 0.8f, { format(Locale.US, "%.2f", it) }, config.pinchStartThreshold
        ) { v ->
            persist {
                it.copy(
                    pinchStartThreshold = v,
                    pinchReleaseThreshold = (v + 0.25f).coerceAtMost(0.95f)
                )
            }
        }

        bindSlider(
            binding.seekClickCooldown, binding.valueClickCooldown,
            100f, 1500f, { format(Locale.US, "%.0f ms", it) }, config.clickCooldownMs.toFloat()
        ) { v -> persist { it.copy(clickCooldownMs = v.toLong()) } }

        bindSlider(
            binding.seekDetectionConfidence, binding.valueDetectionConfidence,
            0.1f, 0.9f, { format(Locale.US, "%.2f", it) }, config.minHandDetectionConfidence
        ) { v ->
            persist {
                it.copy(
                    minHandDetectionConfidence = v,
                    minHandPresenceConfidence = v,
                    minTrackingConfidence = v
                )
            }
        }
    }

    private fun bindSwitches() {
        binding.switchMirror.isChecked = config.mirrorHorizontal
        binding.switchMirror.setOnCheckedChangeListener { _, checked ->
            persist { it.copy(mirrorHorizontal = checked) }
        }

        binding.switchCursor.isChecked = config.showCursor
        binding.switchCursor.setOnCheckedChangeListener { _, checked ->
            persist { it.copy(showCursor = checked) }
        }

        binding.switchEnablePointer.isChecked = config.enablePointer
        binding.switchEnablePointer.setOnCheckedChangeListener { _, checked ->
            persist { it.copy(enablePointer = checked) }
        }

        binding.switchEnableScroll.isChecked = config.enableScroll
        binding.switchEnableScroll.setOnCheckedChangeListener { _, checked ->
            persist { it.copy(enableScroll = checked) }
        }

        binding.switchEnablePinch.isChecked = config.enablePinch
        binding.switchEnablePinch.setOnCheckedChangeListener { _, checked ->
            persist { it.copy(enablePinch = checked) }
        }

        binding.switchEnableFist.isChecked = config.enableFist
        binding.switchEnableFist.setOnCheckedChangeListener { _, checked ->
            persist { it.copy(enableFist = checked) }
        }
    }

    private fun bindSlider(
        seek: SeekBar,
        label: TextView,
        min: Float,
        max: Float,
        format: (Float) -> String,
        current: Float,
        onChanged: (Float) -> Unit
    ) {
        seek.progress = (((current - min) / (max - min)) * 100f).toInt().coerceIn(0, 100)
        label.text = format(current)
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = min + (max - min) * progress / 100f
                label.text = format(value)
                if (fromUser) onChanged(value)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
    }

    private fun format(locale: Locale, pattern: String, value: Float): String =
        String.format(locale, pattern, value)
}
