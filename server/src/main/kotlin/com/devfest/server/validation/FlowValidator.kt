package com.devfest.server.validation

import com.devfest.server.model.FlowBlock
import com.devfest.server.model.FlowGraph
import com.devfest.server.model.FlowGraphResponse
import java.util.Locale

class FlowValidator {
    private val allowedTypes = setOf(
        "LocationExitTrigger",
        "TimeScheduleTrigger",
        "ManualQuickTrigger",
        "TimeWindowCondition",
        "BatteryGuardCondition",
        "ContextMatchCondition",
        "SendNotificationAction",
        "SendSMSAction",
        "HttpWebhookAction",
        "ToggleWifiAction",
        "PlaySoundAction",
        "DelayAction",
        "SetVariableAction",
        "GetVariableBlock",
        "GetVariableBlock",
        "BranchSelector",
        "BatteryLevelCondition",
        // Sensors
        "Pedometer",
        "Camera",
        "Location",
        "ActivityRecognition",
        "SetAlarmAction"
    )

    fun validate(response: FlowGraphResponse) {
        val blockIds = response.graph.blocks.map(FlowBlock::id).toSet()
        response.graph.blocks.forEach { block ->
            require(block.id.isNotBlank()) { "Block id missing" }
            require(block.type in allowedTypes) { "Unsupported block type ${block.type}" }
        }
        response.graph.edges.forEach { edge ->
            require(edge.from in blockIds) { "Edge source ${edge.from} missing" }
            require(edge.to in blockIds) { "Edge target ${edge.to} missing" }
        }
        require(determineStarts(response.graph).isNotEmpty()) { "No trigger blocks found" }
    }

    private fun determineStarts(graph: FlowGraph): List<FlowBlock> =
        graph.blocks.filter { block ->
            block.type.lowercase(Locale.US).contains("trigger")
        }
}
