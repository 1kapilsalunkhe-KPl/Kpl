package com.pixel6a.callrecorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Pixel 6a In-Call Recording Overlay Service
 * Injects a floating Manual Record / Stop button directly onto the active call screen.
 */
class InCallFloatingOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var isRecording = false
    private var recordingStartTime = 0L
    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val audioRecorder by lazy { AudioRecordManager(applicationContext) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundServiceNotification()
        showManualRecordOverlay()
    }

    private fun showManualRecordOverlay() {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 350
        }

        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.overlay_in_call_recorder, null)

        val btnRecord = overlayView?.findViewById<ImageButton>(R.id.btn_manual_record)
        val tvStatus = overlayView?.findViewById<TextView>(R.id.tv_record_status)
        val tvTimer = overlayView?.findViewById<TextView>(R.id.tv_record_timer)

        // Single tap listener to toggle Manual Recording during active call
        btnRecord?.setOnClickListener {
            if (!isRecording) {
                // START MANUAL RECORDING
                val started = audioRecorder.startRecording()
                if (started) {
                    isRecording = true
                    btnRecord.setImageResource(R.drawable.ic_stop_recording)
                    tvStatus?.text = "Recording..."
                    tvStatus?.setTextColor(getColor(R.color.recording_red))
                    recordingStartTime = System.currentTimeMillis()
                    startDurationTimer(tvTimer)
                    Toast.makeText(this, "Call recording started on Pixel 6a", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to capture audio source", Toast.LENGTH_SHORT).show()
                }
            } else {
                // STOP MANUAL RECORDING
                audioRecorder.stopRecording()
                isRecording = false
                btnRecord.setImageResource(R.drawable.ic_mic_start)
                tvStatus?.text = "Saved"
                tvStatus?.setTextColor(getColor(R.color.google_green))
                timerJob?.cancel()
                tvTimer?.text = "00:00"
                Toast.makeText(this, "Call recording saved to Storage/Recordings", Toast.LENGTH_LONG).show()
            }
        }

        // Allow user to drag the button around their screen
        overlayView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var touchX = 0f
            private var touchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        touchX = event.rawX
                        touchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - touchX).toInt()
                        params.y = initialY + (event.rawY - touchY).toInt()
                        windowManager.updateViewLayout(overlayView, params)
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(overlayView, params)
    }

    private fun startDurationTimer(tvTimer: TextView?) {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive && isRecording) {
                val elapsed = (System.currentTimeMillis() - recordingStartTime) / 1000
                val mins = elapsed / 60
                val secs = elapsed % 60
                tvTimer?.text = String.format("%02d:%02d", mins, secs)
                delay(1000)
            }
        }
    }

    private fun startForegroundServiceNotification() {
        val channelId = "pixel6a_call_record_channel"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "In-Call Manual Recording Service",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Pixel 6a In-Call Recorder")
            .setContentText("Tap overlay button during call to start/stop recording")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()

        startForeground(101, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRecording) {
            audioRecorder.stopRecording()
        }
        timerJob?.cancel()
        if (overlayView != null) {
            windowManager.removeView(overlayView)
        }
    }
}
