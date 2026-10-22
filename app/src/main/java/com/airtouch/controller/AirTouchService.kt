package com.airtouch.controller

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService

/**
 * Foreground service that owns the whole control pipeline: the CameraX session,
 * the MediaPipe hand tracker, the gesture engine and the floating cursor.
 *
 * It is a [LifecycleService] so CameraX can bind to a real lifecycle, and it
 * runs as a camera foreground service with a persistent notification. Stopping
 * the service tears everything down immediately.
 */
class AirTouchService : LifecycleService() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val lock = Any()

    private lateinit var settings: SettingsRepository
    private lateinit var calibration: CalibrationManager
    private lateinit var engine: GestureEngine
    private lateinit var overlay: OverlayController
    private lateinit var cameraSession: CameraSession

    private var handTracking: HandTracking? = null
    private var analyzer: CameraFrameAnalyzer? = null

    @Volatile private var screenW = 0
    @Volatile private var screenH = 0

    private var lastCursorX = 0f
    private var lastCursorY = 0f

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        val cfg = settings.load()
        synchronized(lock) { engine.updateConfig(cfg) }
        if (cfg.showCursor) overlay.show() else overlay.hide()
    }

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(this)
        calibration = CalibrationManager(this)
        engine = GestureEngine(settings.load())
        calibration.applyTo(engine)
        overlay = OverlayController(this)
        cameraSession = CameraSession(this, this)

        refreshScreenSize()
        createNotificationChannel()
        startInForeground()
        settings.registerListener(prefListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (handTracking == null) {
            startPipeline()
        }
        return START_NOT_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        refreshScreenSize()
    }

    private fun startPipeline() {
        synchronized(lock) {
            engine.updateConfig(settings.load())
            calibration.applyTo(engine)
            engine.start()
        }
        ControllerBus.updateState(ControllerState.INITIALIZING_CAMERA)
        ControllerBus.updateGesture(GestureType.NONE)

        val cfg = settings.load()
        val tracking = HandTracking(this, cfg) { hand, timestamp -> onHandResult(hand, timestamp) }
        val frameAnalyzer = CameraFrameAnalyzer(tracking) { bitmap ->
            ControllerBus.previewBitmap = bitmap
        }
        handTracking = tracking
        analyzer = frameAnalyzer

        if (cfg.showCursor) overlay.show()

        cameraSession.start(frameAnalyzer) { error ->
            Log.e(TAG, "Camera failed to start", error)
            synchronized(lock) { engine.markError() }
            ControllerBus.updateState(ControllerState.ERROR)
        }
    }

    private fun onHandResult(hand: HandLandmarks?, timestampMs: Long) {
        // Free the analyzer for the next frame before doing any work.
        analyzer?.markIdle()

        val width = screenW
        val height = screenH
        val result = synchronized(lock) {
            engine.onFrame(hand, timestampMs, width, height)
        }
        mainHandler.post { applyResult(result) }
    }

    private fun applyResult(result: GestureResult) {
        ControllerBus.updateState(result.state)
        ControllerBus.updateGesture(result.gesture)

        if (result.hasCursor) {
            lastCursorX = result.cursorX
            lastCursorY = result.cursorY
        }
        overlay.update(lastCursorX, lastCursorY, result.state)

        when (val action = result.action) {
            is GestureAction.Tap ->
                ControlAccessibilityService.instance?.tap(action.x, action.y)

            is GestureAction.Scroll ->
                ControlAccessibilityService.instance?.scroll(lastCursorX, action.dyPx)

            null -> Unit
        }
    }

    override fun onDestroy() {
        settings.unregisterListener(prefListener)
        cameraSession.stop()
        handTracking?.close()
        handTracking = null
        analyzer = null
        overlay.hide()
        synchronized(lock) { engine.stop() }
        ControllerBus.reset()
        super.onDestroy()
    }

    private fun refreshScreenSize() {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            screenW = bounds.width()
            screenH = bounds.height()
        } else {
            @Suppress("DEPRECATION")
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            screenW = metrics.widthPixels
            screenH = metrics.heightPixels
        }
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    private fun startInForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, AirTouchService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setSmallIcon(R.drawable.ic_hand)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(0, getString(R.string.emergency_stop), stopIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        private const val TAG = "AirTouchService"
        private const val CHANNEL_ID = "airtouch_running"
        private const val NOTIF_ID = 4242

        const val ACTION_STOP = "com.airtouch.controller.action.STOP"
    }
}
