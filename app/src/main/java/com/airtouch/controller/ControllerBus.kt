package com.airtouch.controller

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Process-wide, observable controller state. The running [AirTouchService]
 * publishes here; the UI observes here. This keeps the UI decoupled from the
 * service (no binding, no direct references) while still rendering live state.
 */
object ControllerBus {

    private val _state = MutableStateFlow(ControllerState.STOPPED)
    val state: StateFlow<ControllerState> = _state

    private val _gesture = MutableStateFlow(GestureType.NONE)
    val gesture: StateFlow<GestureType> = _gesture

    @Volatile
    var running: Boolean = false
        private set

    /** Latest downscaled camera frame, for the dashboard preview. */
    @Volatile
    var previewBitmap: Bitmap? = null

    fun updateState(newState: ControllerState) {
        _state.value = newState
        running = newState != ControllerState.STOPPED
    }

    fun updateGesture(newGesture: GestureType) {
        _gesture.value = newGesture
    }

    fun reset() {
        _state.value = ControllerState.STOPPED
        _gesture.value = GestureType.NONE
        running = false
        previewBitmap = null
    }
}
