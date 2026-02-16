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
            title = "Intruder Alert",
            blocks = listOf(
                FlowBlock(
                    id = "trigger1",
                    type = "ManualQuickTrigger",
                    params = mapOf("label" to "Start Security")
                ),
                FlowBlock(
                     id = "condition1",
                     type = "Pedometer",
                     params = mapOf("threshold" to "5")
                ),
                FlowBlock(
                    id = "action1",
                    type = "Camera",
                    params = mapOf("lens" to "front")
                ),
                FlowBlock(
                    id = "action2",
                    type = "Location",
                    params = mapOf("accuracy" to "high")
                ),
                FlowBlock(
                    id = "action3",
                    type = "SendNotificationAction",
                    params = mapOf(
                        "title" to "Security Alert",
                        "message" to "Movement detected! Photo taken at location."
                    )
                )
            ),
            edges = listOf(
                FlowEdge(from = "trigger1", to = "condition1", condition = "activate"),
                FlowEdge(from = "condition1", to = "action1", condition = "steps_detected"),
                FlowEdge(from = "action1", to = "action2", condition = "photo_saved"),
                FlowEdge(from = "action2", to = "action3", condition = "location_found")
            ),
            explanation = "If you walk 5 steps, I'll take a selfie, tag your location, and notify you.",
            riskFlags = listOf("Uses Camera", "Tracks Location")
        )
        return FlowGraphResponse(
            flowId = flowId,
            graph = graph,
            explanation = "Demo response: Security scenario with sensors.",
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
        val cleaned = cleanJson(content)
        val jsonElement = json.parseToJsonElement(cleaned)
        
        // Handle case where LLM wraps it in { "graph": ... } or returns direct graph
        val graphElement = if (jsonElement.jsonObject.containsKey("graph")) {
             jsonElement.jsonObject["graph"]!!
        } else {
             jsonElement
        }
        
        return json.decodeFromJsonElement(com.devfest.server.model.FlowGraph.serializer(), graphElement)
    }

    private fun cleanJson(content: String): String {
        return content.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }

    private fun buildPrompt(request: IntentRequest): String = buildString {
        appendLine("User intent: ${request.intentText}")
        appendLine("Capabilities available: ${request.context.capabilities}")
        appendLine("RETURN JSON ONLY. The JSON must match this EXACT structure:")
        appendLine("""
        {
          "graph": {
            "id": "unique_guid",
            "title": "Short Title",
            "blocks": [
              { "id": "b1", "type": "Type", "params": { "k": "v" } }
            ],
            "edges": [
              { "from": "b1", "to": "b2", "condition": "label" }
            ],
            "explanation": "One sentence summary",
            "risk_flags": []
          },
          "explanation": "Same summary as above",
          "risk_flags": []
        }
        """.trimIndent())

        appendLine("IMPORTANT:")
        appendLine("1. Use 'blocks', NOT 'nodes'.")
        appendLine("2. 'graph' object MUST contain 'id', 'title', 'blocks', 'edges', 'explanation'.")
        appendLine("3. Use the exact block types listed below.")
        appendLine("4. EVERY flow MUST start with a Trigger. For immediate commands, use 'ManualQuickTrigger' and link it to the first action.")
        
        appendLine("AVAILABLE BLOCKS:")
        appendLine("1. SENSORS:")
        appendLine("   - Pedometer (params: threshold='10')")
        appendLine("   - BatteryLevelCondition (params: minLevel='15', maxLevel='80')")
        appendLine("   - Camera (params: lens='front'|'back')")
        appendLine("   - Location (params: accuracy='high'|'balanced'|'low')")
        appendLine("   - ActivityRecognition (params: type='STILL'|'WALKING'|'RUNNING', confidence='50')")
        appendLine("2. TRIGGERS:")
        appendLine("   - LocationExitTrigger (params: geofence, radiusMeters)")
        appendLine("   - TimeScheduleTrigger (params: time='HH:mm', days='[\"Mon\"]')")
        appendLine("   - ManualQuickTrigger")
        appendLine("3. ACTIONS & CONDITIONS:")
        appendLine("   - TimeWindowCondition (params: start='HH:mm', end='HH:mm')")
        appendLine("   - BatteryGuardCondition (params: minPercent='15')")
        appendLine("   - SendNotificationAction (params: title, message)")
        appendLine("   - SendSMSAction (params: phone, body)")
        appendLine("   - HttpWebhookAction (params: url, method, body)")
        appendLine("   - ToggleWifiAction (params: enable='true'|'false')")
        appendLine("   - PlaySoundAction (params: uri='content://media/internal/audio/media/1')")
        appendLine("   - SetAlarmAction (params: hour='7', minute='30', message='Wake Up', skipUi='true')")
        appendLine("   - DelayAction (params: millis='1000')")
        
        appendLine("Edges should reference block ids. Condition is optional (empty string if none).")
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
