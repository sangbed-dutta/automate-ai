package com.devfest.automation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devfest.automation.di.AppDependencies
import com.devfest.automation.ui.model.ChatMessage
import com.devfest.automation.ui.model.Role
import com.devfest.data.repository.FlowRepository
import com.devfest.runtime.engine.FlowEngine
import com.devfest.runtime.engine.FlowEngineFactory
import com.devfest.runtime.engine.FlowExecutionInput
import com.devfest.runtime.engine.FlowExecutionResult
import com.devfest.runtime.engine.FlowStepResult
import com.devfest.runtime.model.FlowGraph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.util.UUID

class AgentViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: FlowRepository by lazy {
        AppDependencies.provideFlowRepository(application)
    }

    private val engine: FlowEngine by lazy {
        FlowEngineFactory.createDefault(application)
    }

    private val intentContext = AppDependencies.defaultIntentContext()

    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState

    init {
        viewModelScope.launch {
            repository.observeFlows().collect { flowsWithState ->
                val persistedGraphs = flowsWithState.associate { it.first.id to it.first }
                val activeIds = flowsWithState.filter { it.second }.map { it.first.id }.toSet()
                
                // Merge persisted flows with existing in-memory drafts (if any)
                // Valid persistence should override drafts
                _uiState.value = _uiState.value.copy(
                    flowGraphs = _uiState.value.flowGraphs + persistedGraphs,
                    activeFlowIds = activeIds
                )
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(role = Role.USER, text = text)
        // Add user message immediately
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMessage,
            isLoading = true
        )

        viewModelScope.launch {
            try {
                // Request flow from agent
                val graph = repository.requestFlow(
                    intentText = text,
                    context = intentContext,
                    sessionToken = "demo-session-token"
                )

                // Add agent response with flow
                val agentMessage = ChatMessage(
                    role = Role.AGENT,
                    text = "Here is the automation flow I created for you based on: \"$text\"",
                    flowId = graph.id
                )
                
                // Store draft graph in memory so Chat can see it immediately
                // It won't be in observeFlows yet because we filter isDraft=true
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + agentMessage,
                    flowGraphs = _uiState.value.flowGraphs + (graph.id to graph),
                    isLoading = false
                )
            } catch (ex: Exception) {
                val errorMessage = ChatMessage(
                    role = Role.AGENT,
                    text = "Sorry, I encountered an error: ${ex.localizedMessage ?: "Unknown error"}"
                )
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + errorMessage,
                    isLoading = false
                )
            }
        }
    }

    fun runFlow(graph: FlowGraph) {
        viewModelScope.launch {
            // Reset execution state
            _uiState.value = _uiState.value.copy(
                execution = emptyList(),
                executingFlowId = graph.id,
                statusMessage = "Running flow: ${graph.title}..."
            )

            // Pattern-failure flows need trigger metadata so UnlockFailedConditionHandler passes
            val metadata = if (graph.blocks.any { it.type == com.devfest.runtime.model.BlockType.PATTERN_FAILURE_TRIGGER }) {
                demoMetadata() + ("trigger" to "pattern_failure")
            } else {
                demoMetadata()
            }
            
            val execution = engine.execute(
                graph,
                FlowExecutionInput(
                    metadata = metadata
                )
            )
            
            _uiState.value = _uiState.value.copy(
                statusMessage = "Execution complete.",
                execution = execution.steps
            )
        }
    }

    fun activateFlow(flowId: String) {
        viewModelScope.launch {
            repository.setActive(flowId, true)
            // State update will happen via observeFlows if we map it correctly
            
            // Check if it's a Manual Trigger flow and execute immediately
             val flow = _uiState.value.flowGraphs[flowId]
             if (flow != null && flow.blocks.any { it.type == com.devfest.runtime.model.BlockType.MANUAL_QUICK_TRIGGER }) {
                 runFlow(flow)
             }
        }
    }

    fun toggleFlow(flowId: String, active: Boolean) {
        viewModelScope.launch {
            repository.setActive(flowId, active)
        }
    }

    private fun demoMetadata(): Map<String, String> {
        val now = OffsetDateTime.now().toString()
        return mapOf(
            "local_time" to now,
            "battery_percent" to "80",
            "context" to "weekday"
        )
    }
}

data class AgentUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val statusMessage: String = "Ready",
    val flowGraphs: Map<String, FlowGraph> = emptyMap(),
    val executingFlowId: String? = null,
    val execution: List<FlowStepResult> = emptyList(),
    val activeFlowIds: Set<String> = emptySet()
)
