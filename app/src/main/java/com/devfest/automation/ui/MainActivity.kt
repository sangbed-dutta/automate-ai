package com.devfest.automation.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.devfest.automation.ui.theme.AgentTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgentTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

                    when (currentScreen) {
                        Screen.Dashboard -> DashboardScreen(
                            onNavigateToChat = { currentScreen = Screen.Chat }
                        )
                        Screen.Chat -> ChatScreen(
                            onBack = { currentScreen = Screen.Dashboard },
                            onNavigateToEditor = { flowId ->
                                currentScreen = Screen.FlowEditor(flowId)
                            }
                        )
                        is Screen.FlowEditor -> FlowEditorScreen(
                            flowId = (currentScreen as Screen.FlowEditor).flowId,
                            viewModel = androidx.lifecycle.viewmodel.compose.viewModel(), // Shared VM would be better, but this works if VM is scoped to Activity
                            onBack = { currentScreen = Screen.Chat },
                            onDeploy = { currentScreen = Screen.Dashboard }
                        )
                    }
                }
            }
        }
    }
}

sealed class Screen {
    object Dashboard : Screen()
    object Chat : Screen()
    data class FlowEditor(val flowId: String) : Screen()
}
