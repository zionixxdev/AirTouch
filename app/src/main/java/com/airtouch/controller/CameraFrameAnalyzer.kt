package com.airtouch.controller

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

/**
 * CameraX [ImageAnalysis.Analyzer] that turns camera frames into MediaPipe input.
 *
 * Responsibilities:
 *  - drop frames while a previous frame is still being inferred (back-pressure),
 *  - convert the YUV frame to an upright ARGB bitmap,
 *  - hand a downscaled copy to the UI for the live preview,
 *  - close every [ImageProxy] exactly once.
 */
class CameraFrameAnalyzer(
    private val handTracking: HandTracking,
    private val onPreview: (Bitmap) -> Unit
) : ImageAnalysis.Analyzer {

    @Volatile private var busy = false
    private var busySince = 0L
    private var lastPreviewMs = 0L
    private var lastFrameMs = 0L

    override fun analyze(image: ImageProxy) {
        try {
            val now = SystemClock.uptimeMillis()

            // Back-pressure: skip while MediaPipe is still busy, but never get
            // permanently stuck if a result is lost.
            if (busy && now - busySince < BUSY_TIMEOUT_MS) return
            if (now - lastFrameMs < MIN_FRAME_INTERVAL_MS) return

            lastFrameMs = now
            busy = true
            busySince = now

            val bitmap = image.toBitmap()
            val upright = rotateIfNeeded(bitmap, image.imageInfo.rotationDegrees)

            if (now - lastPreviewMs >= PREVIEW_INTERVAL_MS) {
                lastPreviewMs = now
                val height = (PREVIEW_WIDTH * upright.height / upright.width.toFloat())
                    .toInt().coerceAtLeast(1)
                onPreview(
                    Bitmap.createScaledBitmap(upright, PREVIEW_WIDTH, height, true)
                )
            }

            handTracking.detect(upright, now)
        } catch (t: Throwable) {
            busy = false
            Log.w(TAG, "Frame analysis failed", t)
        } finally {
            image.close()
        }
    }

    /** Called from the MediaPipe result callback once inference for a frame is done. */
    fun markIdle() {
        busy = false
    }

    private fun rotateIfNeeded(source: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return source
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(
            source, 0, 0, source.width, source.height, matrix, true
        )
        if (rotated != source) source.recycle()
        return rotated
    }

    companion object {
        private const val TAG = "AirTouchAnalyzer"
        private const val PREVIEW_WIDTH = 360
        private const val PREVIEW_INTERVAL_MS = 150L
        private const val MIN_FRAME_INTERVAL_MS = 50L
        private const val BUSY_TIMEOUT_MS = 1200L
    }
}
