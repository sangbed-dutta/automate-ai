package com.devfest.automation.receiver

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.app.admin.DeviceAdminReceiver
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.devfest.automation.service.PatternFailureFlowService

/**
 * Device admin receiver that gets notified when the user fails the unlock pattern/password.
 * Starts [PatternFailureFlowService] so the app can run any active flows that react to pattern failure
 * (e.g. take a front-camera photo).
 */
class PatternFailureReceiver : DeviceAdminReceiver() {

    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)
        Log.d(TAG, "Unlock failed – starting pattern-failure flow service")
        val serviceIntent = Intent(context, PatternFailureFlowService::class.java).apply {
            action = PatternFailureFlowService.ACTION_RUN_PATTERN_FAILURE_FLOWS
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start PatternFailureFlowService", e)
        }
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent) {
        super.onPasswordSucceeded(context, intent)
    }

    companion object {
        private const val TAG = "PatternFailureReceiver"

        fun componentName(context: Context): ComponentName =
            ComponentName(context, PatternFailureReceiver::class.java)
    }
}
