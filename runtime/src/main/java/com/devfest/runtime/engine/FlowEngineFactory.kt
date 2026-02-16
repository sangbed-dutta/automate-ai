package com.devfest.runtime.engine

import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import com.devfest.runtime.engine.blocks.*
import com.devfest.runtime.engine.handlers.*
import com.devfest.runtime.model.BlockType

object FlowEngineFactory {
    fun createDefault(context: Context): FlowEngine {
        val handlers = mapOf(
            BlockType.LOCATION_EXIT_TRIGGER to TriggerHandler("Location exit detected"),
            BlockType.TIME_SCHEDULE_TRIGGER to TriggerHandler("Scheduled trigger fired"),
            BlockType.MANUAL_QUICK_TRIGGER to TriggerHandler("Manual trigger fired"),

            BlockType.TIME_WINDOW_CONDITION to TimeWindowConditionHandler(),
            BlockType.BATTERY_GUARD_CONDITION to BatteryGuardHandler(),
            BlockType.BATTERY_LEVEL_CONDITION to BatteryLevelHandler(context),
            BlockType.CONTEXT_MATCH_CONDITION to ContextMatchHandler(),

            BlockType.SEND_NOTIFICATION_ACTION to NotificationHandler(context),
            BlockType.SEND_SMS_ACTION to SmsHandler(),
            BlockType.HTTP_WEBHOOK_ACTION to HttpWebhookHandler(),
            BlockType.TOGGLE_WIFI_ACTION to ToggleWifiHandler(context),
            BlockType.PLAY_SOUND_ACTION to PlaySoundHandler(context),
            
            // Sensor Blocks
            BlockType.PEDOMETER to StepCountHandler(context),
            BlockType.LOCATION to GetLocationHandler(context),
            BlockType.LOCATION to GetLocationHandler(context),
            BlockType.CAMERA to CameraCaptureHandler(context, ProcessLifecycleOwner.get()),
            BlockType.SET_ALARM_ACTION to SetAlarmHandler(context),

            BlockType.DELAY_ACTION to DelayHandler(),
            BlockType.SET_VARIABLE_ACTION to VariableHandler(),
            BlockType.GET_VARIABLE_BLOCK to VariableHandler(),
            BlockType.BRANCH_SELECTOR to BranchSelectorHandler()
        )
        return FlowEngine(handlers)
    }
}
