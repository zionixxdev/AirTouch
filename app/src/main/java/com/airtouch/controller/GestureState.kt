package com.airtouch.controller

/** Explicit controller states for the gesture state machine. */
enum class ControllerState {
    STOPPED,
    INITIALIZING_CAMERA,
    POINTER,
    SCROLL,
    PINCH_PRESSED,
    PAUSED,
    TRACKING_LOST,
    ERROR
}

/** Gestures the engine can recognise. */
enum class GestureType {
    NONE,
    OPEN_PALM,
    INDEX_POINT,
    PINCH,
    FIST
}

/** A discrete action the controller must perform on the device. */
sealed class GestureAction {
    /** Tap at absolute screen coordinates in pixels. */
    data class Tap(val x: Float, val y: Float) : GestureAction()

    /** Scroll the content by [dyPx]; positive scrolls the content downward. */
    data class Scroll(val dyPx: Float) : GestureAction()
}

/** Result of processing a single frame through the engine. */
data class GestureResult(
    val state: ControllerState,
    val gesture: GestureType,
    val cursorX: Float,
    val cursorY: Float,
    val hasCursor: Boolean,
    val action: GestureAction? = null
)
