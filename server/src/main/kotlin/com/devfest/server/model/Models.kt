package com.devfest.server.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IntentRequest(
    @SerialName("user_id") val userId: String,
    @SerialName("intent_text") val intentText: String,
    val context: IntentContext,
    @SerialName("session_token") val sessionToken: String
)

@Serializable
data class IntentContext(
    @SerialName("location_aliases") val locationAliases: List<String> = emptyList(),
    val capabilities: List<String> = emptyList(),
    @SerialName("time_window") val timeWindow: TimeWindow? = null,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class TimeWindow(
    val tz: String,
    val now: String
)

@Serializable
data class FlowGraphResponse(
    @SerialName("flow_id") val flowId: String,
    val graph: FlowGraph,
    val explanation: String,
    @SerialName("risk_flags") val riskFlags: List<String> = emptyList()
)

@Serializable
data class FlowGraph(
    val id: String,
    val title: String,
    val blocks: List<FlowBlock>,
    val edges: List<FlowEdge>,
    val explanation: String,
    @SerialName("risk_flags") val riskFlags: List<String> = emptyList()
)

@Serializable
data class FlowBlock(
    val id: String,
    val type: String,
    val params: Map<String, String> = emptyMap()
)

@Serializable
data class FlowEdge(
    val from: String,
    val to: String,
    val condition: String
)
