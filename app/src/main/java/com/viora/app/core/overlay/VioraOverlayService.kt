package com.viora.app.core.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import com.viora.app.BuildConfig
import com.viora.app.MainActivity

class VioraOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var indicator: TextView? = null
    private var warningView: VioraWarningOverlayView? = null

    override fun onCreate() {
        super.onCreate()
        if (!canDrawOverlays(this)) {
            stopSelf()
            return
        }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showIndicator()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_TEST_WARNING -> showSuspiciousWarning()
            ACTION_SHOW_WARNING -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: return START_STICKY
                val explanation = intent.getStringExtra(EXTRA_EXPLANATION) ?: return START_STICKY
                val riskName = intent.getStringExtra(EXTRA_RISK_LEVEL) ?: return START_STICKY
                val riskLevel = runCatching { VioraRiskLevel.valueOf(riskName) }.getOrNull()
                    ?: return START_STICKY
                showWarning(VioraWarning(title, explanation, riskLevel))
            }
        }
        return START_STICKY
    }

    fun showSuspiciousWarning() {
        showWarning(
            VioraWarning(
                title = "Potential Scam",
                explanation = "This message contains signs of financial or social engineering.",
                riskLevel = VioraRiskLevel.SUSPICIOUS
            )
        )
    }

    fun showWarning(warning: VioraWarning) {
        removeWarning()
        // "View Details" and "Dismiss" are distinct actions: Dismiss only closes the
        // overlay, View Details also brings Viora to the foreground so the user can
        // review the full assessment (Home -> History) before deciding STOP/CONTINUE.
        val view = VioraWarningOverlayView(this, warning, onViewDetails = ::openApp, onDismiss = ::removeWarning)
        warningView = view
        windowManager.addView(view, warningLayoutParams())
    }

    private fun openApp() {
        removeWarning()
        startActivity(
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    override fun onDestroy() {
        removeWarning()
        indicator?.let { runCatching { windowManager.removeView(it) } }
        indicator = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showIndicator() {
        val view = TextView(this).apply {
            text = "Viora"
            setTextColor(Color.WHITE)
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundColor(Color.rgb(0, 145, 160))
            setOnClickListener { showSuspiciousWarning() }
        }
        indicator = view
        windowManager.addView(view, indicatorLayoutParams())
        if (BuildConfig.DEBUG) Log.d(TAG, "Overlay service active")
    }

    private fun removeWarning() {
        warningView?.let { runCatching { windowManager.removeView(it) } }
        warningView = null
    }

    private fun indicatorLayoutParams() = WindowManager.LayoutParams(
        dp(72), dp(40), overlayType(),
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.END
        y = dp(56)
        x = dp(12)
    }

    private fun warningLayoutParams() = WindowManager.LayoutParams(
        dp(320), WindowManager.LayoutParams.WRAP_CONTENT, overlayType(),
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        y = dp(112)
    }

    private fun overlayType() = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val ACTION_SHOW_TEST_WARNING = "com.viora.app.action.SHOW_TEST_WARNING"
        private const val ACTION_SHOW_WARNING = "com.viora.app.action.SHOW_WARNING"
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_EXPLANATION = "extra_explanation"
        private const val EXTRA_RISK_LEVEL = "extra_risk_level"
        private const val TAG = "VioraOverlay"

        fun canDrawOverlays(context: Context) = Settings.canDrawOverlays(context)

        fun start(context: Context) {
            context.startService(Intent(context, VioraOverlayService::class.java))
        }

        fun showWarning(context: Context, warning: VioraWarning) {
            context.startService(Intent(context, VioraOverlayService::class.java).apply {
                action = ACTION_SHOW_WARNING
                putExtra(EXTRA_TITLE, warning.title)
                putExtra(EXTRA_EXPLANATION, warning.explanation)
                putExtra(EXTRA_RISK_LEVEL, warning.riskLevel.name)
            })
        }
    }
}