package com.devfest.server.service

import com.devfest.server.model.FlowBlock
import com.devfest.server.model.FlowEdge
import com.devfest.server.model.FlowGraph
import com.devfest.server.model.FlowGraphResponse
import com.devfest.server.model.IntentRequest
import com.devfest.server.validation.FlowValidator
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import java.time.Instant
import java.util.UUID

class FlowSynthesisService(
    private val openAiKey: String?,
    private val json: Json,
    private val validator: FlowValidator,
    private val gateway: OpenAiGateway = OpenAiGateway(openAiKey, json)
) {

    suspend fun synthesize(request: IntentRequest): FlowGraphResponse {
        val response = if (openAiKey.isNullOrBlank()) {
            demoFlow(request)
        } else {
            val generated = gateway.generateFlow(request)
            FlowGraphResponse(
                flowId = generated.flowId ?: UUID.randomUUID().toString(),
                graph = generated.graph,
                explanation = generated.explanation ?: "Generated via OpenAI at ${Instant.now()}",
                riskFlags = generated.riskFlags ?: emptyList()
            )
        }
        validator.validate(response)
        return response
    }

    private fun demoFlow(request: IntentRequest): FlowGraphResponse {
        val flowId = UUID.randomUUID().toString()
        val graph = FlowGraph(
            id = UUID.randomUUID().toString(),
            title = "Leave Work Comfort Kit",
            blocks = listOf(
                FlowBlock(
                    id = "trigger1",
                    type = "LocationExitTrigger",
                    params = mapOf(
                        "geofence" to "office",
                        "radiusMeters" to "150"
                    )
                ),
                FlowBlock(
                    id = "condition1",
                    type = "TimeWindowCondition",
                    params = mapOf(
                        "start" to "17:00",
                        "end" to "23:00"
                    )
                ),
                FlowBlock(
                    id = "action1",
                    type = "HttpWebhookAction",
                    params = mapOf(
                        "url" to "https://smart-home.example.com/ac/on",
                        "method" to "POST"
                    )
                ),
                FlowBlock(
                    id = "action2",
                    type = "SendNotificationAction",
                    params = mapOf(
                        "title" to "Automation Complete",
                        "message" to "AC on and partner notified."
                    )
                )
            ),
            edges = listOf(
                FlowEdge(from = "trigger1", to = "condition1", condition = "onExit"),
                FlowEdge(from = "condition1", to = "action1", condition = "withinWindow"),
                FlowEdge(from = "condition1", to = "action2", condition = "withinWindow")
            ),
            explanation = "Leaving the office after 5 PM turns on AC and shows a notification.",
            riskFlags = listOf("Requires location permission", "Calls external webhook")
        )
        return FlowGraphResponse(
            flowId = flowId,
            graph = graph,
            explanation = "Demo response because OPENAI_API_KEY not set.",
            riskFlags = listOf("Demo mode")
        )
    }
}

class OpenAiGateway(
    private val apiKey: String?,
    private val json: Json
) {
    private val client = HttpClientFactory.build()

    suspend fun generateFlow(request: IntentRequest): GeneratedFlow {
        require(!apiKey.isNullOrBlank()) { "Missing OpenAI API key" }
        val prompt = buildPrompt(request)
        val response = client.post("https://api.openai.com/v1/chat/completions") {
            headers.append("Authorization", "Bearer $apiKey")
            headers.append("Content-Type", "application/json")
            setBody(
                ChatCompletionRequest(
                    model = "gpt-4.1-mini",
                    messages = listOf(
                        Message(role = "system", content = SYSTEM_PROMPT),
                        Message(role = "user", content = prompt)
                    ),
                    temperature = 0.2
                )
            )
        }
        val payload = response.body<ChatCompletionResponse>()
        val content = payload.choices.firstOrNull()?.message?.content ?: error("No content from OpenAI")
        val flowGraph = parseGraph(content)
        return GeneratedFlow(
            flowId = payload.id,
            graph = flowGraph,
            explanation = "Generated via OpenAI at ${Instant.now()}",
            riskFlags = listOf("AI generated")
        )
    }

    private fun parseGraph(content: String): com.devfest.server.model.FlowGraph {
        val jsonElement = JsonParser.evaluate(content)
        val graph = jsonElement.jsonObject["graph"] ?: jsonElement
        return json.decodeFromJsonElement(com.devfest.server.model.FlowGraph.serializer(), graph)
    }

    private fun buildPrompt(request: IntentRequest): String = buildString {
        appendLine("User intent: ${request.intentText}")
        appendLine("Capabilities available: ${request.context.capabilities}")
        appendLine("Return a JSON object with fields flow_id(optional), graph, explanation, risk_flags.")
        appendLine("graph must match this Kotlin schema: ${com.devfest.server.model.FlowGraph.serializer().descriptor.serialName}")
        appendLine("Blocks must use only these types: LocationExitTrigger, TimeScheduleTrigger, ManualQuickTrigger, TimeWindowCondition, BatteryGuardCondition, ContextMatchCondition, SendNotificationAction, SendSMSAction, HttpWebhookAction, ToggleWifiAction, PlaySoundAction, DelayAction, SetVariableAction, GetVariableBlock, BranchSelector.")
        appendLine("Edges should reference block ids, include condition labels, and avoid infinite loops.")
    }

    companion object {
        private const val SYSTEM_PROMPT =
            "You are an automation architect returning strict JSON for mobile automation flows. " +
                "You must never include explanations outside the JSON payload."
    }
}

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double = 0.2
)

@Serializable
data class Message(
    val role: String,
    val content: String
)

@Serializable
data class ChatCompletionResponse(
    val id: String,
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val message: MessageContent
)

@Serializable
data class MessageContent(
    val role: String,
    val content: String
)

data class GeneratedFlow(
    val flowId: String?,
    val graph: com.devfest.server.model.FlowGraph,
    val explanation: String?,
    val riskFlags: List<String>?
)
