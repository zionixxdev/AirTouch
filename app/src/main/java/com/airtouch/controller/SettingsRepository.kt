package com.airtouch.controller

import android.content.Context
import android.content.SharedPreferences

/**
 * Local persistence for every user-tunable value, backed by [SharedPreferences].
 * Nothing here touches the network.
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): GestureConfig {
        val d = GestureConfig.DEFAULT
        return GestureConfig(
            pointerSensitivity = prefs.getFloat(KEY_PTR_SENS, d.pointerSensitivity),
            pointerSmoothing = prefs.getFloat(KEY_PTR_SMOOTH, d.pointerSmoothing),
            pointerDeadZonePx = prefs.getFloat(KEY_PTR_DEAD, d.pointerDeadZonePx),
            scrollSensitivity = prefs.getFloat(KEY_SCR_SENS, d.scrollSensitivity),
            scrollDeadZone = prefs.getFloat(KEY_SCR_DEAD, d.scrollDeadZone),
            scrollMinIntervalMs = prefs.getLong(KEY_SCR_INTERVAL, d.scrollMinIntervalMs),
            pinchStartThreshold = prefs.getFloat(KEY_PINCH_START, d.pinchStartThreshold),
            pinchReleaseThreshold = prefs.getFloat(KEY_PINCH_RELEASE, d.pinchReleaseThreshold),
            clickCooldownMs = prefs.getLong(KEY_CLICK_CD, d.clickCooldownMs),
            minHandDetectionConfidence = prefs.getFloat(KEY_CONF_DETECT, d.minHandDetectionConfidence),
            minHandPresenceConfidence = prefs.getFloat(KEY_CONF_PRESENCE, d.minHandPresenceConfidence),
            minTrackingConfidence = prefs.getFloat(KEY_CONF_TRACK, d.minTrackingConfidence),
            mirrorHorizontal = prefs.getBoolean(KEY_MIRROR, d.mirrorHorizontal),
            showCursor = prefs.getBoolean(KEY_SHOW_CURSOR, d.showCursor),
            enablePointer = prefs.getBoolean(KEY_EN_POINTER, d.enablePointer),
            enableScroll = prefs.getBoolean(KEY_EN_SCROLL, d.enableScroll),
            enablePinch = prefs.getBoolean(KEY_EN_PINCH, d.enablePinch),
            enableFist = prefs.getBoolean(KEY_EN_FIST, d.enableFist)
        )
    }

    fun save(config: GestureConfig) {
        prefs.edit()
            .putFloat(KEY_PTR_SENS, config.pointerSensitivity)
            .putFloat(KEY_PTR_SMOOTH, config.pointerSmoothing)
            .putFloat(KEY_PTR_DEAD, config.pointerDeadZonePx)
            .putFloat(KEY_SCR_SENS, config.scrollSensitivity)
            .putFloat(KEY_SCR_DEAD, config.scrollDeadZone)
            .putLong(KEY_SCR_INTERVAL, config.scrollMinIntervalMs)
            .putFloat(KEY_PINCH_START, config.pinchStartThreshold)
            .putFloat(KEY_PINCH_RELEASE, config.pinchReleaseThreshold)
            .putLong(KEY_CLICK_CD, config.clickCooldownMs)
            .putFloat(KEY_CONF_DETECT, config.minHandDetectionConfidence)
            .putFloat(KEY_CONF_PRESENCE, config.minHandPresenceConfidence)
            .putFloat(KEY_CONF_TRACK, config.minTrackingConfidence)
            .putBoolean(KEY_MIRROR, config.mirrorHorizontal)
            .putBoolean(KEY_SHOW_CURSOR, config.showCursor)
            .putBoolean(KEY_EN_POINTER, config.enablePointer)
            .putBoolean(KEY_EN_SCROLL, config.enableScroll)
            .putBoolean(KEY_EN_PINCH, config.enablePinch)
            .putBoolean(KEY_EN_FIST, config.enableFist)
            .apply()
    }

    fun resetToDefaults() {
        save(GestureConfig.DEFAULT)
    }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        private const val PREFS_NAME = "airtouch_settings"
        private const val KEY_PTR_SENS = "pointer_sensitivity"
        private const val KEY_PTR_SMOOTH = "pointer_smoothing"
        private const val KEY_PTR_DEAD = "pointer_dead_zone"
        private const val KEY_SCR_SENS = "scroll_sensitivity"
        private const val KEY_SCR_DEAD = "scroll_dead_zone"
        private const val KEY_SCR_INTERVAL = "scroll_min_interval"
        private const val KEY_PINCH_START = "pinch_start"
        private const val KEY_PINCH_RELEASE = "pinch_release"
        private const val KEY_CLICK_CD = "click_cooldown"
        private const val KEY_CONF_DETECT = "confidence_detection"
        private const val KEY_CONF_PRESENCE = "confidence_presence"
        private const val KEY_CONF_TRACK = "confidence_tracking"
        private const val KEY_MIRROR = "mirror_horizontal"
        private const val KEY_SHOW_CURSOR = "show_cursor"
        private const val KEY_EN_POINTER = "enable_pointer"
        private const val KEY_EN_SCROLL = "enable_scroll"
        private const val KEY_EN_PINCH = "enable_pinch"
        private const val KEY_EN_FIST = "enable_fist"
    }
}
