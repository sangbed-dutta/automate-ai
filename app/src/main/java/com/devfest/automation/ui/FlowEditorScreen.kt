package com.devfest.automation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devfest.automation.ui.components.FlowGraphView
import com.devfest.automation.viewmodel.AgentViewModel
import com.devfest.runtime.model.FlowGraph

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowEditorScreen(
    flowId: String,
    viewModel: AgentViewModel,
    onBack: () -> Unit,
    onDeploy: () -> Unit
) {
    // Find the flow in the viewmodel
    val graph = viewModel.uiState.collectAsState().value.flowGraphs[flowId]

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Review Flow",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "● AI Generated",
                            style = MaterialTheme.typography.labelSmall,
                            color = com.devfest.automation.ui.theme.ActionGreen
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Tools: Add, Undo, Redo
                IconButton(
                    onClick = { /* Add manual block */ },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Block")
                }
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = { /* Undo */ }) {
                    Icon(Icons.Default.Undo, contentDescription = "Undo")
                }
                IconButton(onClick = { /* Redo */ }) {
                    Icon(Icons.Default.Redo, contentDescription = "Redo")
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Button(
                    onClick = {
                        graph?.let {
                            viewModel.runFlow(it)
                            onDeploy() // Could go to dashboard or stay here showing 'Running'
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Deploy Flow 🚀")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (graph != null) {
                FlowGraphView(
                    graph = graph,
                    modifier = Modifier.fillMaxSize(),
                    nodeWidth = 160.dp,
                    nodeHeight = 70.dp
                )
                
                // Permission Warning Banner (Mock logic based on risk flags)
                if (graph.riskFlags.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF332B00) // Dark Yellowish
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add, // Should be Warning icon
                                contentDescription = "Warning",
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "PERMISSION REQUIRED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFFC107),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "This automation requires background ${graph.riskFlags.joinToString()}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Flow not found", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
