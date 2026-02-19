package com.devfest.automation.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.app.admin.DevicePolicyManager
import com.devfest.automation.receiver.PatternFailureReceiver

/**
 * Helpers for device admin used by pattern-failure flows (e.g. capture photo on wrong unlock).
 */
object DeviceAdminHelper {

    fun componentName(context: Context): ComponentName =
        PatternFailureReceiver.componentName(context)

    fun isDeviceAdminEnabled(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isAdminActive(componentName(context))
    }

    /**
     * Intent to prompt the user to add this app as a device admin.
     * Must be started from an Activity (e.g. startActivityForResult).
     * 
     * @param explanation Optional custom explanation. If null, uses a generic message.
     */
    fun createAddDeviceAdminIntent(context: Context, explanation: String? = null): Intent {
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName(context))
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                explanation ?: "Allow this app to run your automation flows that require device admin access (e.g. security flows that react to unlock failures)."
            )
        }
    }
}
