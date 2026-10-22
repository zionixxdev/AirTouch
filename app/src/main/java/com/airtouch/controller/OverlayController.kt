package com.airtouch.controller

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.content.ContextCompat

/**
 * Lightweight floating cursor drawn with [WindowManager] and
 * `TYPE_APPLICATION_OVERLAY`. The overlay is non-focusable and non-touchable so
 * it never steals input, and every WindowManager call is marshalled to the main
 * thread.
 */
class OverlayController(private val context: Context) {

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var view: View? = null
    private var cursorImage: ImageView? = null
    private var params: WindowManager.LayoutParams? = null
    private val sizePx = context.resources.getDimensionPixelSize(R.dimen.cursor_size)

    fun canDraw(): Boolean = Settings.canDrawOverlays(context)

    fun show() {
        mainHandler.post {
            if (view != null || !canDraw()) return@post
            val inflated = LayoutInflater.from(context).inflate(R.layout.view_cursor, null)
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
            }
            try {
                windowManager.addView(inflated, lp)
                view = inflated
                cursorImage = inflated.findViewById(R.id.ivCursor)
                params = lp
            } catch (_: Throwable) {
                // Overlay permission revoked between check and add — ignore.
            }
        }
    }

    fun update(x: Float, y: Float, state: ControllerState) {
        mainHandler.post {
            val v = view ?: return@post
            val lp = params ?: return@post
            lp.x = (x - sizePx / 2f).toInt()
            lp.y = (y - sizePx / 2f).toInt()
            try {
                windowManager.updateViewLayout(v, lp)
            } catch (_: Throwable) {
                // View detached (rotation, permission change) — drop this update.
            }
            cursorImage?.setColorFilter(colorFor(state))
        }
    }

    fun hide() {
        mainHandler.post {
            val v = view ?: return@post
            try {
                windowManager.removeView(v)
            } catch (_: Throwable) {
                // Already detached.
            }
            view = null
            cursorImage = null
            params = null
        }
    }

    private fun colorFor(state: ControllerState): Int = ContextCompat.getColor(
        context,
        when (state) {
            ControllerState.SCROLL -> R.color.cursor_scroll
            ControllerState.PINCH_PRESSED -> R.color.cursor_pinch
            ControllerState.PAUSED, ControllerState.TRACKING_LOST -> R.color.cursor_paused
            else -> R.color.cursor_default
        }
    )
}
