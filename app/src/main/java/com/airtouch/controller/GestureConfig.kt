package com.airtouch.controller

/**
 * Every tunable value the gesture engine and camera pipeline use.
 * Persisted by [SettingsRepository]; the engine is stateless with respect to
 * storage and simply receives a new instance whenever settings change.
 */
data class GestureConfig(
    // Pointer
    val pointerSensitivity: Float = 1.6f,
    val pointerSmoothing: Float = 0.35f,
    val pointerDeadZonePx: Float = 6f,

    // Scroll
    val scrollSensitivity: Float = 1.2f,
    val scrollDeadZone: Float = 0.04f,
    val scrollMinIntervalMs: Long = 120L,

    // Pinch / click
    val pinchStartThreshold: Float = 0.45f,
    val pinchReleaseThreshold: Float = 0.72f,
    val clickCooldownMs: Long = 500L,

    // MediaPipe confidence
    val minHandDetectionConfidence: Float = 0.5f,
    val minHandPresenceConfidence: Float = 0.5f,
    val minTrackingConfidence: Float = 0.5f,

    // Presentation
    val mirrorHorizontal: Boolean = true,
    val showCursor: Boolean = true,

    // Per-gesture enable switches
    val enablePointer: Boolean = true,
    val enableScroll: Boolean = true,
    val enablePinch: Boolean = true,
    val enableFist: Boolean = true,

    // Temporal stability
    val stableFrames: Int = 3,
    val lostFrames: Int = 6
) {
    companion object {
        val DEFAULT = GestureConfig()
    }
}
