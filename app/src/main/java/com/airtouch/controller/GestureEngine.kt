package com.airtouch.controller

import kotlin.math.abs

/**
 * The gesture interpretation engine.
 *
 * This class is deliberately free of Android dependencies: feed it one
 * [HandLandmarks] per processed frame plus the screen size, and it returns the
 * controller state, the recognised gesture, the cursor position and any
 * discrete action that must be dispatched. That keeps it unit-testable on the
 * JVM and independent of the camera, the UI and the accessibility service.
 */
class GestureEngine(private var config: GestureConfig) {

    private var state: ControllerState = ControllerState.STOPPED
    private var gesture: GestureType = GestureType.NONE

    // Temporal debounce
    private var candidate: GestureType = GestureType.NONE
    private var candidateCount = 0
    private var lostCount = 0

    // Cursor smoothing
    private var smoothedX = 0f
    private var smoothedY = 0f
    private var haveSmoothed = false

    // Calibration: the normalized palm centre treated as "neutral".
    private var calibX = Float.NaN
    private var calibY = Float.NaN

    // Pinch
    private var pinchLatched = false
    private var lastClickMs = 0L

    // Scroll
    private var scrollAnchorY = Float.NaN
    private var lastScrollMs = 0L

    // ---- lifecycle -------------------------------------------------------

    fun start() {
        state = ControllerState.INITIALIZING_CAMERA
        gesture = GestureType.NONE
        resetTransient()
    }

    fun stop() {
        state = ControllerState.STOPPED
        gesture = GestureType.NONE
        resetTransient()
    }

    fun markError() {
        state = ControllerState.ERROR
        gesture = GestureType.NONE
        resetTransient()
    }

    fun updateConfig(newConfig: GestureConfig) {
        config = newConfig
    }

    fun currentState(): ControllerState = state

    fun setCalibration(x: Float, y: Float) {
        calibX = x
        calibY = y
    }

    fun clearCalibration() {
        calibX = Float.NaN
        calibY = Float.NaN
    }

    private fun resetTransient() {
        candidate = GestureType.NONE
        candidateCount = 0
        lostCount = 0
        haveSmoothed = false
        pinchLatched = false
        scrollAnchorY = Float.NaN
    }

    // ---- per-frame entry point ------------------------------------------

    fun onFrame(
        hand: HandLandmarks?,
        timestampMs: Long,
        screenW: Int,
        screenH: Int
    ): GestureResult {
        if (state == ControllerState.STOPPED || state == ControllerState.ERROR) {
            return GestureResult(state, GestureType.NONE, 0f, 0f, false)
        }

        if (hand == null || hand.size < 21) {
            lostCount++
            if (lostCount >= config.lostFrames && state != ControllerState.TRACKING_LOST) {
                state = ControllerState.TRACKING_LOST
                gesture = GestureType.NONE
                pinchLatched = false
                scrollAnchorY = Float.NaN
            }
            // Freeze the cursor and emit nothing while the hand is gone.
            return GestureResult(state, GestureType.NONE, 0f, 0f, false)
        }

        lostCount = 0
        val features = computeFeatures(hand)
        val confirmed = stableGesture(features.rawGesture)
        if (confirmed != GestureType.NONE) gesture = confirmed

        // Emergency pause is immediate — no debounce, no cooldown.
        if (features.rawGesture == GestureType.FIST && config.enableFist) {
            if (state != ControllerState.PAUSED) {
                state = ControllerState.PAUSED
                pinchLatched = false
                scrollAnchorY = Float.NaN
            }
            gesture = GestureType.FIST
            return GestureResult(ControllerState.PAUSED, gesture, 0f, 0f, false)
        }

        return when (state) {
            ControllerState.INITIALIZING_CAMERA,
            ControllerState.TRACKING_LOST ->
                enterActive(confirmed, hand, timestampMs, screenW, screenH, state)

            ControllerState.PAUSED ->
                enterActive(confirmed, hand, timestampMs, screenW, screenH, ControllerState.PAUSED)

            ControllerState.POINTER ->
                handlePointer(features, confirmed, hand, screenW, screenH)

            ControllerState.SCROLL ->
                handleScroll(features, confirmed, hand, timestampMs, screenW, screenH)

            ControllerState.PINCH_PRESSED ->
                handlePinchPressed(features, hand, timestampMs, screenW, screenH)

            else -> GestureResult(state, gesture, 0f, 0f, false)
        }
    }

    // ---- feature extraction ---------------------------------------------

    private class Features(
        val pinchRatio: Float,
        val rawGesture: GestureType
    )

    private fun computeFeatures(hand: HandLandmarks): Features {
        val palm = hand.palmSize()
        val pinchRatio = hand.distance(HandLandmarks.THUMB_TIP, HandLandmarks.INDEX_TIP) / palm

        val indexExt = isExtended(hand, HandLandmarks.INDEX_TIP, HandLandmarks.INDEX_PIP)
        val middleExt = isExtended(hand, HandLandmarks.MIDDLE_TIP, HandLandmarks.MIDDLE_PIP)
        val ringExt = isExtended(hand, HandLandmarks.RING_TIP, HandLandmarks.RING_PIP)
        val pinkyExt = isExtended(hand, HandLandmarks.PINKY_TIP, HandLandmarks.PINKY_PIP)
        val count = listOf(indexExt, middleExt, ringExt, pinkyExt).count { it }

        // Pinch takes priority: a pinch otherwise looks like "index only".
        val raw = when {
            pinchRatio < config.pinchStartThreshold -> GestureType.PINCH
            count == 0 -> GestureType.FIST
            count == 4 -> GestureType.OPEN_PALM
            count == 1 && indexExt -> GestureType.INDEX_POINT
            else -> GestureType.NONE
        }
        return Features(pinchRatio, raw)
    }

    private fun isExtended(hand: HandLandmarks, tip: Int, pip: Int): Boolean {
        val dTip = hand.distance(HandLandmarks.WRIST, tip)
        val dPip = hand.distance(HandLandmarks.WRIST, pip)
        return dTip > dPip * 1.08f
    }

    /** Require the same gesture for [GestureConfig.stableFrames] frames. */
    private fun stableGesture(raw: GestureType): GestureType {
        if (raw == candidate) {
            if (candidateCount < config.stableFrames) candidateCount++
        } else {
            candidate = raw
            candidateCount = 1
        }
        return if (candidateCount >= config.stableFrames) candidate else GestureType.NONE
    }

    // ---- state handlers --------------------------------------------------

    private fun enterActive(
        confirmed: GestureType,
        hand: HandLandmarks,
        timestampMs: Long,
        screenW: Int,
        screenH: Int,
        fallback: ControllerState
    ): GestureResult {
        when {
            confirmed == GestureType.OPEN_PALM && config.enablePointer -> {
                state = ControllerState.POINTER
                gesture = GestureType.OPEN_PALM
                haveSmoothed = false
                pinchLatched = false
                return pointerResult(hand, screenW, screenH)
            }
            confirmed == GestureType.INDEX_POINT && config.enableScroll -> {
                state = ControllerState.SCROLL
                gesture = GestureType.INDEX_POINT
                scrollAnchorY = Float.NaN
                return GestureResult(state, gesture, 0f, 0f, false)
            }
        }
        return GestureResult(fallback, gesture, 0f, 0f, false)
    }

    private fun handlePointer(
        features: Features,
        confirmed: GestureType,
        hand: HandLandmarks,
        screenW: Int,
        screenH: Int
    ): GestureResult {
        if (confirmed == GestureType.INDEX_POINT && config.enableScroll) {
            state = ControllerState.SCROLL
            gesture = GestureType.INDEX_POINT
            scrollAnchorY = Float.NaN
            return GestureResult(state, gesture, 0f, 0f, false)
        }

        if (config.enablePinch && !pinchLatched && features.pinchRatio < config.pinchStartThreshold) {
            pinchLatched = true
            state = ControllerState.PINCH_PRESSED
            val (cx, cy) = cursorFor(hand, screenW, screenH)
            return GestureResult(state, GestureType.PINCH, cx, cy, true)
        }

        return pointerResult(hand, screenW, screenH)
    }

    private fun handlePinchPressed(
        features: Features,
        hand: HandLandmarks,
        timestampMs: Long,
        screenW: Int,
        screenH: Int
    ): GestureResult {
        val (cx, cy) = cursorFor(hand, screenW, screenH)
        // Hysteresis: only release past the (larger) release threshold.
        if (features.pinchRatio > config.pinchReleaseThreshold) {
            pinchLatched = false
            state = ControllerState.POINTER
            gesture = GestureType.OPEN_PALM
            val action = if (timestampMs - lastClickMs >= config.clickCooldownMs) {
                lastClickMs = timestampMs
                GestureAction.Tap(cx, cy)
            } else {
                null
            }
            return GestureResult(state, gesture, cx, cy, true, action)
        }
        return GestureResult(state, GestureType.PINCH, cx, cy, true)
    }

    private fun handleScroll(
        features: Features,
        confirmed: GestureType,
        hand: HandLandmarks,
        timestampMs: Long,
        screenW: Int,
        screenH: Int
    ): GestureResult {
        if (confirmed == GestureType.OPEN_PALM && config.enablePointer) {
            state = ControllerState.POINTER
            gesture = GestureType.OPEN_PALM
            haveSmoothed = false
            return pointerResult(hand, screenW, screenH)
        }
        if (confirmed != GestureType.INDEX_POINT && confirmed != GestureType.NONE) {
            // Ambiguous / unsupported — hold position without acting.
            return GestureResult(state, gesture, 0f, 0f, false)
        }

        val indexY = hand[HandLandmarks.INDEX_TIP].y
        if (scrollAnchorY.isNaN()) {
            scrollAnchorY = indexY
            return GestureResult(state, gesture, 0f, 0f, false)
        }

        val dy = indexY - scrollAnchorY // image y grows downward
        if (abs(dy) < config.scrollDeadZone) {
            return GestureResult(state, gesture, 0f, 0f, false)
        }
        if (timestampMs - lastScrollMs < config.scrollMinIntervalMs) {
            return GestureResult(state, gesture, 0f, 0f, false)
        }

        lastScrollMs = timestampMs
        scrollAnchorY = indexY

        val magnitude = abs(dy) * config.scrollSensitivity * screenH
        // Finger moves up (dy < 0) -> scroll the content down.
        val dyPx = if (dy < 0f) magnitude else -magnitude
        return GestureResult(state, gesture, 0f, 0f, false, GestureAction.Scroll(dyPx))
    }

    // ---- cursor mapping --------------------------------------------------

    private fun pointerResult(hand: HandLandmarks, screenW: Int, screenH: Int): GestureResult {
        val (cx, cy) = cursorFor(hand, screenW, screenH)
        return GestureResult(state, gesture, cx, cy, true)
    }

    /** Map the palm centre to screen pixels with calibration, sensitivity and smoothing. */
    private fun cursorFor(hand: HandLandmarks, screenW: Int, screenH: Int): Pair<Float, Float> {
        val centre = hand.palmCentre()
        val refX = if (calibX.isNaN()) 0.5f else calibX
        val refY = if (calibY.isNaN()) 0.5f else calibY

        var nx = 0.5f + (centre.x - refX) * config.pointerSensitivity
        var ny = 0.5f + (centre.y - refY) * config.pointerSensitivity
        nx = nx.coerceIn(0f, 1f)
        ny = ny.coerceIn(0f, 1f)

        val mirroredX = if (config.mirrorHorizontal) 1f - nx else nx
        val targetX = mirroredX * screenW
        val targetY = ny * screenH

        if (!haveSmoothed) {
            smoothedX = targetX
            smoothedY = targetY
            haveSmoothed = true
        } else {
            val alpha = config.pointerSmoothing.coerceIn(0.05f, 1f)
            if (abs(targetX - smoothedX) > config.pointerDeadZonePx) {
                smoothedX += (targetX - smoothedX) * alpha
            }
            if (abs(targetY - smoothedY) > config.pointerDeadZonePx) {
                smoothedY += (targetY - smoothedY) * alpha
            }
        }
        return smoothedX to smoothedY
    }
}
