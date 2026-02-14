package com.devfest.automation.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devfest.automation.ui.components.FlowPreview
import com.devfest.automation.ui.theme.AgentTheme
import com.devfest.automation.viewmodel.AgentViewModel
import com.devfest.runtime.engine.FlowStepStatus

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgentTheme {
                AgentAutomatorApp()
            }
        }
    }
}

@Composable
private fun AgentAutomatorApp(
    viewModel: AgentViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Agent Automator",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = uiState.statusMessage,
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = viewModel::triggerDemoFlow) {
                Text("Run Demo Flow")
            }
            uiState.flowGraph?.let { graph ->
                FlowPreview(
                    title = graph.title,
                    explanation = graph.explanation,
                    blocks = graph.blocks.map { it.type.name to it.params }
                )
                Divider()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f, fill = true),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.execution) { step ->
                        val status = when (step.status) {
                            FlowStepStatus.SUCCESS -> "✅"
                            FlowStepStatus.SKIPPED -> "⚠️"
                            FlowStepStatus.FAILED -> "❌"
                        }
                        Text(
                            text = "$status ${step.blockId}: ${step.message}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
