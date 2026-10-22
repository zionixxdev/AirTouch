package com.airtouch.controller

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

/**
 * Thin wrapper around the MediaPipe Hand Landmarker running in LIVE_STREAM mode.
 *
 * Everything happens on-device with the bundled `hand_landmarker.task` model:
 * no frames, landmarks or telemetry ever leave the phone, and no API key is
 * required. Results are delivered on MediaPipe's own worker thread, so the
 * callback must be cheap and thread-safe.
 */
class HandTracking(
    context: Context,
    config: GestureConfig,
    private val onResult: (HandLandmarks?, Long) -> Unit
) {

    private val landmarker: HandLandmarker = build(context, config)

    private fun build(context: Context, config: GestureConfig): HandLandmarker {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(MODEL_ASSET)
            .setDelegate(Delegate.CPU)
            .build()

        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumHands(1)
            .setMinHandDetectionConfidence(config.minHandDetectionConfidence)
            .setMinHandPresenceConfidence(config.minHandPresenceConfidence)
            .setMinTrackingConfidence(config.minTrackingConfidence)
            .setResultListener { result, _ -> onResult(parse(result), result.timestampMs()) }
            .setErrorListener { _ -> onResult(null, 0L) }
            .build()

        return HandLandmarker.createFromOptions(context, options)
    }

    /** Submit one upright frame for asynchronous inference. */
    fun detect(bitmap: Bitmap, timestampMs: Long) {
        val image: MPImage = BitmapImageBuilder(bitmap).build()
        landmarker.detectAsync(image, timestampMs)
    }

    fun close() {
        landmarker.close()
    }

    private fun parse(result: HandLandmarkerResult): HandLandmarks? {
        val hands = result.landmarks()
        if (hands.isEmpty()) return null
        val lms = hands[0]
        if (lms.size < 21) return null
        return HandLandmarks(lms.map { Landmark(it.x(), it.y(), it.z()) })
    }

    companion object {
        const val MODEL_ASSET = "hand_landmarker.task"
    }
}
