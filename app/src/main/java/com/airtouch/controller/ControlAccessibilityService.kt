package com.airtouch.controller

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlin.math.abs

/**
 * Accessibility service that turns controller actions into real touches.
 *
 * It only ever performs gestures the user asked for, only while AirTouch is
 * running, and it never reads window content. Protected system screens, secure
 * windows, permission dialogs and some games may refuse simulated input — that
 * is a platform limitation, not something AirTouch can work around.
 */
class ControlAccessibilityService : AccessibilityService() {

    @Volatile
    private var busy = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "Accessibility service connected")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used — AirTouch never inspects screen content.
    }

    override fun onInterrupt() {
        // Not used.
    }

    /** Single tap at absolute screen coordinates. */
    fun tap(x: Float, y: Float): Boolean {
        if (busy) return false
        val path = Path().apply { moveTo(x, y) }
        return dispatch(GestureDescription.StrokeDescription(path, 0L, TAP_DURATION_MS))
    }

    /**
     * Swipe to scroll. [dyPx] is the desired content movement: positive scrolls
     * the content downward, which is a swipe from lower to upper on screen.
     */
    fun scroll(x: Float, y: Float, dyPx: Float): Boolean {
        if (busy) return false
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels.toFloat()
        val height = metrics.heightPixels.toFloat()
        val margin = height * 0.06f
        val maxSwipe = height * 0.35f

        var delta = dyPx.coerceIn(-maxSwipe, maxSwipe)
        if (abs(delta) < height * 0.02f) {
            delta = if (delta >= 0f) height * 0.02f else -height * 0.02f
        }

        // Choose a start point so both ends of the swipe stay on screen.
        val lower = margin + maxOf(0f, delta)
        val upper = (height - margin) + minOf(0f, delta)
        val startY = y.coerceIn(lower, maxOf(lower, upper))
        val endY = startY - delta
        val startX = x.coerceIn(margin, width - margin)

        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(startX, endY)
        }
        return dispatch(GestureDescription.StrokeDescription(path, 0L, SCROLL_DURATION_MS))
    }

    private fun dispatch(stroke: GestureDescription.StrokeDescription): Boolean {
        busy = true
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        val accepted = dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    busy = false
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    busy = false
                }
            },
            null
        )
        if (!accepted) busy = false
        return accepted
    }

    companion object {
        private const val TAG = "AirTouchAccessibility"
        private const val TAP_DURATION_MS = 50L
        private const val SCROLL_DURATION_MS = 120L

        @Volatile
        var instance: ControlAccessibilityService? = null
            private set

        fun isConnected(): Boolean = instance != null
    }
}
