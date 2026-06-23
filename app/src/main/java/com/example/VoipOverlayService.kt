package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class VoipOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var voipManager: VoipManager? = null
    
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    companion object {
        private const val NOTIFICATION_ID = 9981
        private const val CHANNEL_ID = "voip_overlay_service_channel"

        fun start(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                return
            }
            val intent = Intent(context, VoipOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, VoipOverlayService::class.java)
            context.stopService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        voipManager = VoipManager.instance
        if (voipManager == null) {
            stopSelf()
            return
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            showOverlay()
        } else {
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Overlay Call Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "نمایش پاپ‌آپ تماس فعال خارج از برنامه"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("تماس VoIP فعال")
            .setContentText("در حال مکالمه در پس‌زمینه")
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val wmParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = dpToPx(16) // Spacing from top
        }

        // Custom programmatic CardView overlay
        val card = FrameLayout(this).apply {
            val gd = GradientDrawable().apply {
                setColor(Color.parseColor("#0F172A")) // Slate 900
                cornerRadius = dpToPx(24).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#334155")) // Slate 700 border
            }
            background = gd
            elevation = dpToPx(8).toFloat()
            setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        // 1. Live indicator dot
        val dot = View(this).apply {
            val dotGd = GradientDrawable().apply {
                setColor(Color.parseColor("#10B981")) // Green 500
                shape = GradientDrawable.OVAL
            }
            background = dotGd
            layoutParams = LinearLayout.LayoutParams(dpToPx(8), dpToPx(8)).apply {
                leftMargin = dpToPx(8)
                rightMargin = dpToPx(8)
            }
        }
        container.addView(dot)

        // 2. Text column (Caller number & Duration)
        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.RIGHT
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = dpToPx(12)
                leftMargin = dpToPx(12)
            }
        }

        val numberTextView = TextView(this).apply {
            text = "تماس در جریان"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            paint.isFakeBoldText = true
        }
        textContainer.addView(numberTextView)

        val durationTextView = TextView(this).apply {
            text = "00:00"
            setTextColor(Color.parseColor("#94A3B8")) // Slate 400
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        }
        textContainer.addView(durationTextView)
        container.addView(textContainer)

        // 3. Hangup Button
        val hangupButton = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.WHITE)
            val btnGd = GradientDrawable().apply {
                setColor(Color.parseColor("#EF4444")) // Red 500
                shape = GradientDrawable.OVAL
            }
            background = btnGd
            setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6))
            layoutParams = LinearLayout.LayoutParams(dpToPx(32), dpToPx(32))
            isClickable = true
            isFocusable = true
        }
        container.addView(hangupButton)

        card.addView(container)
        overlayView = card

        // Click on layout -> Return to app
        card.setOnClickListener {
            val appIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(appIntent)
        }

        // Hangup Action
        hangupButton.setOnClickListener {
            voipManager?.endCall()
        }

        // Make overlay draggable
        card.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = wmParams.x
                    initialY = wmParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    wmParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    wmParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    try {
                        windowManager?.updateViewLayout(view, wmParams)
                    } catch (e: Exception) {}
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Check if it was a simple click instead of drag
                    val diffX = kotlin.math.abs(event.rawX - initialTouchX)
                    val diffY = kotlin.math.abs(event.rawY - initialTouchY)
                    if (diffX < 10 && diffY < 10) {
                        view.performClick()
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(card, wmParams)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
            return
        }

        // Start observing VoIP state and update overlay UI
        serviceScope.launch {
            voipManager?.callState?.collectLatest { state ->
                if (state == VoipCallState.IDLE || state == VoipCallState.DISCONNECTED) {
                    stopSelf()
                }
            }
        }

        serviceScope.launch {
            voipManager?.activeCallNumber?.collectLatest { number ->
                numberTextView.text = if (number.isNotBlank()) "مکالمه با: $number" else "تماس اینترنتی فعال"
            }
        }

        serviceScope.launch {
            voipManager?.callDuration?.collectLatest { duration ->
                val minutes = duration / 60
                val seconds = duration % 60
                durationTextView.text = String.format("%02d:%02d", minutes, seconds)
            }
        }

        // Blinking indicator effect
        val handler = Handler(Looper.getMainLooper())
        val blinkRunnable = object : Runnable {
            override fun run() {
                dot.visibility = if (dot.visibility == View.VISIBLE) View.INVISIBLE else View.VISIBLE
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(blinkRunnable)
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {}
        }
    }
}
