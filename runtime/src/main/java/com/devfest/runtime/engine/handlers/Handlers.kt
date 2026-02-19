package com.devfest.runtime.engine.handlers

import android.content.Context
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.devfest.runtime.engine.FlowBlockHandler
import com.devfest.runtime.engine.FlowExecutionInput
import com.devfest.runtime.engine.FlowExecutionState
import com.devfest.runtime.engine.FlowStepResult
import com.devfest.runtime.engine.FlowStepStatus
import com.devfest.runtime.model.BlockType
import com.devfest.runtime.model.FlowBlock
import com.devfest.runtime.notifications.NotificationChannels
import kotlinx.coroutines.delay
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.content.Intent

private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

class TriggerHandler(
    private val description: String
) : FlowBlockHandler {
    override suspend fun handle(
        block: FlowBlock,
        input: FlowExecutionInput,
        state: FlowExecutionState
    ): FlowStepResult = FlowStepResult(
        blockId = block.id,
        status = FlowStepStatus.SUCCESS,
        message = description
    )
}

class TimeWindowConditionHandler : FlowBlockHandler {
    override suspend fun handle(
        block: FlowBlock,
        input: FlowExecutionInput,
        state: FlowExecutionState
    ): FlowStepResult {
        val nowIso = input.metadata["local_time"] ?: return FlowStepResult(
            blockId = block.id,
            status = FlowStepStatus.SKIPPED,
            message = "Missing local time metadata"
        )
        val start = block.params["start"] ?: "00:00"
        val end = block.params["end"] ?: "23:59"
        val now = LocalTime.parse(nowIso.substring(11, 16))
        val within = now in LocalTime.parse(start, TIME_FORMATTER)..LocalTime.parse(end, TIME_FORMATTER)
        return FlowStepResult(
            blockId = block.id,
            status = if (within) FlowStepStatus.SUCCESS else FlowStepStatus.SKIPPED,
            message = if (within) "Within $start-$end window" else "Outside window $start-$end"
        )
    }
}

class BatteryGuardHandler : FlowBlockHandler {
    override suspend fun handle(
        block: FlowBlock,
        input: FlowExecutionInput,
        state: FlowExecutionState
    ): FlowStepResult {
        val threshold = block.params["minPercent"]?.toIntOrNull() ?: 30
        val percent = input.metadata["battery_percent"]?.toIntOrNull()
        val ok = percent == null || percent >= threshold
        return FlowStepResult(
            blockId = block.id,
            status = if (ok) FlowStepStatus.SUCCESS else FlowStepStatus.SKIPPED,
            message = if (ok) "Battery OK (${percent ?: "unknown"}%)" else "Battery too low ($percent%)"
        )
    }
}

class ContextMatchHandler : FlowBlockHandler {
    override suspend fun handle(
        block: FlowBlock,
        input: FlowExecutionInput,
        state: FlowExecutionState
    ): FlowStepResult {
        val target = block.params["value"]?.lowercase() ?: return FlowStepResult(
            blockId = block.id,
            status = FlowStepStatus.SKIPPED,
            message = "No target context"
        )
        val actual = input.metadata["context"]?.lowercase()
        val matches = actual?.contains(target) == true
        return FlowStepResult(
            blockId = block.id,
            status = if (matches) FlowStepStatus.SUCCESS else FlowStepStatus.SKIPPED,
            message = if (matches) "Context matched '$target'" else "Context '$actual' != '$target'"
        )
    }
}

/**
 * Condition that passes when the flow was triggered by a failed unlock/pattern attempt.
 * Expects execution metadata "trigger" = "pattern_failure" (set by app when starting flow from DeviceAdminReceiver).
 */
class UnlockFailedConditionHandler : FlowBlockHandler {
    override suspend fun handle(
        block: FlowBlock,
        input: FlowExecutionInput,
        state: FlowExecutionState
    ): FlowStepResult {
        val trigger = input.metadata["trigger"]?.lowercase()
        val passed = trigger == "pattern_failure" || trigger == "unlock_failed"
        return FlowStepResult(
            blockId = block.id,
            status = if (passed) FlowStepStatus.SUCCESS else FlowStepStatus.SKIPPED,
            message = if (passed) "Unlock failed context confirmed" else "Not triggered by failed unlock (trigger=$trigger)"
        )
    }
}

class NotificationHandler(
    private val context: Context
) : FlowBlockHandler {
    override suspend fun handle(
        block: FlowBlock,
        input: FlowExecutionInput,
        state: FlowExecutionState
    ): FlowStepResult {
        NotificationChannels.ensureChannel(context)
        val builder = NotificationCompat.Builder(context, NotificationChannels.FLOW_CHANNEL_ID)
            .setContentTitle(block.params["title"] ?: "Agent Automator")
            .setContentText(block.params["message"] ?: "Automation executed.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        NotificationManagerCompat.from(context).notify(block.id.hashCode(), builder.build())
        return FlowStepResult(block.id, FlowStepStatus.SUCCESS, "Notification shown")
    }
}

class HttpWebhookHandler : FlowBlockHandler {
    override suspend fun handle(
        block: FlowBlock,
        input: FlowExecutionInput,
        state: FlowExecutionState
    ): FlowStepResult {
        val url = block.params["url"] ?: return FlowStepResult(
            block.id,
            FlowStepStatus.FAILED,
            "Missing URL"
        )
        val method = block.params["method"]?.uppercase() ?: "POST"
        val body = block.params["body"] ?: ""

        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = method
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.doOutput = body.isNotEmpty()
            connection.setRequestProperty("Content-Type", "application/json")
            if (body.isNotEmpty()) {
                connection.outputStream.use { os ->
                    os.write(body.toByteArray())
                }
            }
            val code = connection.responseCode
            FlowStepResult(block.id, FlowStepStatus.SUCCESS, "Webhook $code")
        } catch (ex: Exception) {
            FlowStepResult(block.id, FlowStepStatus.FAILED, ex.localizedMessage ?: "Webhook failed")
        }
    }
}

class ToggleWifiHandler(
    private val context: Context
) : FlowBlockHandler {
    override suspend fun handle(
        block: FlowBlock,
        input: FlowExecutionInput,
        state: FlowExecutionState
    ): FlowStepResult {
        val applicationContext = context.applicationContext
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val enable = block.params["enable"]?.toBoolean() ?: true // Default to ON if not specified, though usually we'd want 'toggle' or specific state

        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                // Legacy method for Android 9 and below
                @Suppress("DEPRECATION")
                val success = wm.setWifiEnabled(enable)
                if (success) {
                    FlowStepResult(block.id, FlowStepStatus.SUCCESS, "Wi-Fi state set to $enable")
                } else {
                    FlowStepResult(block.id, FlowStepStatus.FAILED, "Failed to set Wi-Fi state")
                }
            } else {
                // Android 10+ restriction: Open Settings Panel
                val panelIntent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                applicationContext.startActivity(panelIntent)
                FlowStepResult(block.id, FlowStepStatus.SUCCESS, "Opened Internet Panel (Android 10+ restriction)")
            }
        } catch (e: Exception) {
             FlowStepResult(block.id, FlowStepStatus.FAILED, "Error handling Wi-Fi: ${e.message}")
        }
    }
}

class PlaySoundHandler(
    private val context: Context
) : FlowBlockHandler {
    override suspend fun handle(
        block: FlowBlock,
        input: FlowExecutionInput,
        state: FlowExecutionState
    ): FlowStepResult {
        val toneUri = block.params["uri"] ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString()
        return try {
            RingtoneManager.getRingtone(context, Uri.parse(toneUri))?.play()
            FlowStepResult(block.id, FlowStepStatus.SUCCESS, "Sound played")
        } catch (ex: Exception) {
            FlowStepResult(block.id, FlowStepStatus.FAILED, ex.localizedMessage ?: "Sound failed")
        }
    }
}

class DelayHandler : FlowBlockHandler {
    override suspend fun handle(
        block: FlowBlock,
        input: FlowExecutionInput,
        state: FlowExecutionState
    ): FlowStepResult {
        val millis = block.params["millis"]?.toLongOrNull() ?: 0L
        if (millis > 0) delay(millis)
        return FlowStepResult(block.id, FlowStepStatus.SUCCESS, "Delayed for $millis ms")
    }
}

class VariableHandler : FlowBlockHandler {
    override suspend fun handle(
        block: FlowBlock,
        input: FlowExecutionInput,
        state: FlowExecutionState
    ): FlowStepResult {
        return when (block.type) {
            BlockType.SET_VARIABLE_ACTION -> {
                val key = block.params["key"] ?: return FlowStepResult(block.id, FlowStepStatus.FAILED, "Missing key")
                state.variables[key] = block.params["value"].orEmpty()
                FlowStepResult(block.id, FlowStepStatus.SUCCESS, "Set $key")
            }
            BlockType.GET_VARIABLE_BLOCK -> {
                val key = block.params["key"] ?: return FlowStepResult(block.id, FlowStepStatus.FAILED, "Missing key")
                val value = state.variables[key]
                FlowStepResult(block.id, FlowStepStatus.SUCCESS, "Value for $key = ${value ?: "null"}")
            }
            else -> FlowStepResult(block.id, FlowStepStatus.SKIPPED, "Unsupported variable op")
        }
    }
}

class BranchSelectorHandler : FlowBlockHandler {
    override suspend fun handle(
        block: FlowBlock,
        input: FlowExecutionInput,
        state: FlowExecutionState
    ): FlowStepResult = FlowStepResult(
        block.id,
        FlowStepStatus.SUCCESS,
        "Branch evaluated (${block.params["route"] ?: "default"})"
    )
}

class SmsHandler : FlowBlockHandler {
    override suspend fun handle(
        block: FlowBlock,
        input: FlowExecutionInput,
        state: FlowExecutionState
    ): FlowStepResult {
        val phone = block.params["phone"] ?: return FlowStepResult(block.id, FlowStepStatus.SKIPPED, "Missing phone")
        val body = block.params["body"] ?: "Automation triggered."
        return try {
            SmsManager.getDefault().sendTextMessage(phone, null, body, null, null)
            FlowStepResult(block.id, FlowStepStatus.SUCCESS, "SMS sent")
        } catch (ex: SecurityException) {
            FlowStepResult(block.id, FlowStepStatus.SKIPPED, "SMS permission required")
        } catch (ex: Exception) {
            FlowStepResult(block.id, FlowStepStatus.FAILED, ex.localizedMessage ?: "SMS failed")
        }
    }
}
