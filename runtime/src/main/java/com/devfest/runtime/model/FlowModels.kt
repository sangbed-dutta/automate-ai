package com.devfest.runtime.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FlowGraph(
    val id: String,
    val title: String,
    val blocks: List<FlowBlock>,
    val edges: List<FlowEdge>,
    val explanation: String,
    val riskFlags: List<String> = emptyList()
)

@Serializable
data class FlowBlock(
    val id: String,
    val type: BlockType,
    val params: Map<String, String> = emptyMap()
)

@Serializable
data class FlowEdge(
    val from: String,
    val to: String,
    val condition: String
)

@Serializable
enum class BlockCategory {
    TRIGGER,
    CONDITION,
    ACTION,
    UTILITY
}

@Serializable
enum class BlockType(val category: BlockCategory) {
    @SerialName("LocationExitTrigger")
    LOCATION_EXIT_TRIGGER(BlockCategory.TRIGGER),
    @SerialName("TimeScheduleTrigger")
    TIME_SCHEDULE_TRIGGER(BlockCategory.TRIGGER),
    @SerialName("ManualQuickTrigger")
    MANUAL_QUICK_TRIGGER(BlockCategory.TRIGGER),

    @SerialName("TimeWindowCondition")
    TIME_WINDOW_CONDITION(BlockCategory.CONDITION),
    @SerialName("BatteryGuardCondition")
    BATTERY_GUARD_CONDITION(BlockCategory.CONDITION),
    @SerialName("ContextMatchCondition")
    CONTEXT_MATCH_CONDITION(BlockCategory.CONDITION),

    @SerialName("SendNotificationAction")
    SEND_NOTIFICATION_ACTION(BlockCategory.ACTION),
    @SerialName("SendSMSAction")
    SEND_SMS_ACTION(BlockCategory.ACTION),
    @SerialName("HttpWebhookAction")
    HTTP_WEBHOOK_ACTION(BlockCategory.ACTION),
    @SerialName("ToggleWifiAction")
    TOGGLE_WIFI_ACTION(BlockCategory.ACTION),
    @SerialName("PlaySoundAction")
    PLAY_SOUND_ACTION(BlockCategory.ACTION),

    @SerialName("DelayAction")
    DELAY_ACTION(BlockCategory.UTILITY),
    @SerialName("SetVariableAction")
    SET_VARIABLE_ACTION(BlockCategory.UTILITY),
    @SerialName("GetVariableBlock")
    GET_VARIABLE_BLOCK(BlockCategory.UTILITY),
    @SerialName("BranchSelector")
    BRANCH_SELECTOR(BlockCategory.UTILITY)
}
