package com.airtouch.controller

import android.content.Context

/**
 * Stores the user's neutral hand position so the pointer can be re-centred.
 * Calibration is optional: when it has never been captured the engine simply
 * uses the image centre as its reference.
 */
class CalibrationManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val isCalibrated: Boolean
        get() = prefs.getBoolean(KEY_DONE, false)

    fun neutralX(): Float = prefs.getFloat(KEY_X, Float.NaN)

    fun neutralY(): Float = prefs.getFloat(KEY_Y, Float.NaN)

    fun save(centre: Landmark) {
        prefs.edit()
            .putFloat(KEY_X, centre.x)
            .putFloat(KEY_Y, centre.y)
            .putBoolean(KEY_DONE, true)
            .apply()
    }

    fun reset() {
        prefs.edit().clear().apply()
    }

    /** Push the stored neutral position into a freshly created engine. */
    fun applyTo(engine: GestureEngine) {
        val x = neutralX()
        val y = neutralY()
        if (!x.isNaN() && !y.isNaN()) {
            engine.setCalibration(x, y)
        } else {
            engine.clearCalibration()
        }
    }

    companion object {
        private const val PREFS_NAME = "airtouch_calibration"
        private const val KEY_X = "neutral_x"
        private const val KEY_Y = "neutral_y"
        private const val KEY_DONE = "calibrated"
    }
}
