package com.devfest.automation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devfest.automation.di.AppDependencies
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

    fun triggerDemoFlow() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                statusMessage = "Synthesizing flow...",
                flowGraph = null,
                execution = emptyList()
            )
            try {
                val graph = repository.requestFlow(
                    intentText = "When I leave work after 5pm, turn on AC and notify partner.",
                    context = intentContext,
                    sessionToken = "demo-session-token"
                )
                val execution = engine.execute(
                    graph,
                    FlowExecutionInput(
                        metadata = demoMetadata()
                    )
                )
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Flow ready: ${graph.title}",
                    flowGraph = graph,
                    execution = execution.steps
                )
            } catch (ex: Exception) {
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Failed to build flow: ${ex.localizedMessage}"
                )
            }
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
    val statusMessage: String = "Describe an automation to get started.",
    val flowGraph: FlowGraph? = null,
    val execution: List<FlowStepResult> = emptyList()
)
