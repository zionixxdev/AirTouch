package com.airtouch.controller

import kotlin.math.hypot

/** A single normalized landmark in image space (x, y in 0..1). */
data class Landmark(val x: Float, val y: Float, val z: Float = 0f)

/**
 * Immutable snapshot of one detected hand: the 21 MediaPipe hand landmarks.
 * Index constants follow the MediaPipe hand topology so callers can stay
 * readable instead of sprinkling magic numbers everywhere.
 */
class HandLandmarks(val points: List<Landmark>) {

    val size: Int get() = points.size

    operator fun get(index: Int): Landmark = points[index]

    /** Average of the five palm landmarks — a stable "palm centre". */
    fun palmCentre(): Landmark {
        val ids = intArrayOf(WRIST, INDEX_MCP, MIDDLE_MCP, RING_MCP, PINKY_MCP)
        var sx = 0f
        var sy = 0f
        var sz = 0f
        for (id in ids) {
            sx += points[id].x
            sy += points[id].y
            sz += points[id].z
        }
        val n = ids.size.toFloat()
        return Landmark(sx / n, sy / n, sz / n)
    }

    /** Distance between two landmarks in normalized image space. */
    fun distance(a: Int, b: Int): Float {
        val p = points[a]
        val q = points[b]
        return hypot(p.x - q.x, p.y - q.y)
    }

    /**
     * Reference palm size, used to make pinch thresholds independent of how far
     * the hand is from the camera. Guarded against divide-by-zero.
     */
    fun palmSize(): Float = distance(WRIST, MIDDLE_MCP).coerceAtLeast(1e-4f)

    companion object {
        const val WRIST = 0
        const val THUMB_CMC = 1
        const val THUMB_MCP = 2
        const val THUMB_IP = 3
        const val THUMB_TIP = 4
        const val INDEX_MCP = 5
        const val INDEX_PIP = 6
        const val INDEX_DIP = 7
        const val INDEX_TIP = 8
        const val MIDDLE_MCP = 9
        const val MIDDLE_PIP = 10
        const val MIDDLE_DIP = 11
        const val MIDDLE_TIP = 12
        const val RING_MCP = 13
        const val RING_PIP = 14
        const val RING_DIP = 15
        const val RING_TIP = 16
        const val PINKY_MCP = 17
        const val PINKY_PIP = 18
        const val PINKY_DIP = 19
        const val PINKY_TIP = 20
    }
}
