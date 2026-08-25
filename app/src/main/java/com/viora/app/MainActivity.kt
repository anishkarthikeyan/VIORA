package com.viora.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.IntentCompat
import com.viora.app.core.navigation.VioraNavigation
import com.viora.app.core.overlay.VioraOverlayService
import com.viora.app.presentation.components.VioraTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainActivity : ComponentActivity() {

    /** Opens the system screen where the user grants Viora overlay permission. */
    fun requestOverlayPermission() {
        if (!VioraOverlayService.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            })
        }
    }

    /** Starts the MVP indicator after overlay permission has been granted. */
    fun startVioraOverlay() {
        if (VioraOverlayService.canDrawOverlays(this)) {
            VioraOverlayService.start(this)
        }
    }

    /**
     * Latest text received via the Android Share Sheet (ACTION_SEND, text/plain).
     * Consumed and cleared by [VioraNavigation]; null means nothing pending.
     */
    private val _sharedText = MutableStateFlow<String?>(null)
    val sharedText: StateFlow<String?> = _sharedText.asStateFlow()

    /**
     * Latest image received via the Android Share Sheet (ACTION_SEND with an
     * image mime type). Consumed and cleared by [VioraNavigation]; null means nothing pending.
     */
    private val _sharedImage = MutableStateFlow<Uri?>(null)
    val sharedImage: StateFlow<Uri?> = _sharedImage.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleSendIntent(intent)
        setContent {
            VioraTheme {
                VioraNavigation(sharedText = sharedText, sharedImage = sharedImage)
            }
        }
    }

    /** Fires when VIORA is already running and the user shares into it (singleTask). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSendIntent(intent)
    }

    private fun handleSendIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        when (intent.type) {
            "text/plain" ->
                // Malformed/empty shares simply produce null/blank — the processor handles that.
                _sharedText.value = intent.getStringExtra(Intent.EXTRA_TEXT)

            null -> Unit // unknown share type — ignore gracefully

            else -> {
                if (intent.type!!.startsWith("image/")) {
                    _sharedImage.value = IntentCompat.getParcelableExtra(
                        intent,
                        Intent.EXTRA_STREAM,
                        Uri::class.java
                    )
                }
                // Any other mime type is not supported yet; ignored on purpose.
            }
        }
    }

    /** Marks the pending text share as consumed so it is not reprocessed. */
    fun clearSharedText() {
        _sharedText.value = null
    }

    /** Marks the pending image share as consumed so it is not reprocessed. */
    fun clearSharedImage() {
        _sharedImage.value = null
    }
}
