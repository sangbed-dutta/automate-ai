package com.devfest.agentclient.model

import com.devfest.runtime.model.FlowGraph
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
    @SerialName("graph") val graph: FlowGraph,
    @SerialName("explanation") val explanation: String,
    @SerialName("risk_flags") val riskFlags: List<String> = emptyList()
)
