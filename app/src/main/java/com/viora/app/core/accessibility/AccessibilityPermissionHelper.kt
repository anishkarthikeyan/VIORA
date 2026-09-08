package com.viora.app.core.accessibility

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Reads and opens Android's own accessibility-service enablement — Viora never
 * enables the service itself (Android does not allow that without user action in
 * system Settings); this only reports current status and deep-links there.
 */
object AccessibilityPermissionHelper {

    /** True if the user has explicitly enabled [VioraAccessibilityService] in system Settings. */
    fun isEnabled(context: Context): Boolean {
        val expected = ComponentName(context, VioraAccessibilityService::class.java).flattenToString()
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    /** Opens the system screen where the user can turn Viora's protection on/off. */
    fun openSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
