package com.devfest.automation.ui

import android.app.Activity
import android.content.pm.PackageManager
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.devfest.automation.ui.theme.ActionGreen
import com.devfest.automation.ui.theme.ElectricBlue
import com.devfest.automation.ui.theme.TriggerBlue
import com.devfest.automation.util.DeviceAdminHelper
import com.devfest.automation.util.FlowPermissionHelper
import com.devfest.runtime.model.BlockType

@Composable
fun DashboardScreen(
    viewModel: com.devfest.automation.viewmodel.AgentViewModel,
    onNavigateToChat: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            DashboardHeader()

            LazyColumn(
                contentPadding = PaddingValues(bottom = 100.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Stats Widget
                item {
                    StatsSection(viewModel)
                }

                // Active Flows
                item {
                    SectionHeader(title = "Your Flows", action = "View All") // Renamed title
                    ActiveFlowsList(viewModel)
                }

                // Recent Activity
                item {
                    SectionHeader(title = "Recent Activity")
                    RecentActivityLog()
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = onNavigateToChat,
            containerColor = ElectricBlue,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(Icons.Filled.Bolt, contentDescription = "Quick Trigger")
        }
    }
}

@Composable
fun DashboardHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(screenPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar Placeholder
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Welcome back",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Command Center",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Row {
            IconButton(onClick = { /* TODO */ }) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
            }
        }
    }
}

@Composable
fun StatsSection(viewModel: com.devfest.automation.viewmodel.AgentViewModel) {
    val uiState = viewModel.uiState.collectAsState().value
    val activeCount = uiState.activeFlowIds.size
    val totalFlows = uiState.flowGraphs.size
    val successRate = if (totalFlows > 0) "100%" else "-" // Mock success rate for now

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        item {
            StatCard(
                label = "Active",
                value = "$activeCount",
                subtext = "$totalFlows created",
                subtextColor = ActionGreen,
                icon = Icons.Filled.ArrowUpward
            )
        }
        item {
            StatCard(
                label = "Savings",
                value = "${activeCount * 0.5}h", // Mock calc
                subtext = "This week",
                subtextColor = MaterialTheme.colorScheme.primary
            )
        }
        item {
            StatCard(
                label = "Success",
                value = successRate,
                subtext = "Stable",
                subtextColor = ActionGreen
            )
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    subtext: String,
    subtextColor: Color,
    icon: ImageVector? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.width(140.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = subtextColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtextColor
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (action != null) {
            Text(
                text = action,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ActiveFlowsList(viewModel: com.devfest.automation.viewmodel.AgentViewModel) {
    val uiState = viewModel.uiState.collectAsState().value
    val context = LocalContext.current
    val activity = context as? Activity
    var pendingActivateFlowId by remember { mutableStateOf<String?>(null) }
    var pendingActivateNeedsCamera by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (pendingActivateFlowId != null) {
            // Only activate if permission was granted
            if (granted) {
                viewModel.toggleFlow(pendingActivateFlowId!!, true)
            }
            pendingActivateFlowId = null
            pendingActivateNeedsCamera = false
        }
    }

    val deviceAdminLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && pendingActivateFlowId != null) {
            val flowId = pendingActivateFlowId!!
            val needsCamera = pendingActivateNeedsCamera &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
            if (needsCamera) {
                cameraLauncher.launch(Manifest.permission.CAMERA)
            } else {
                viewModel.toggleFlow(flowId, true)
                pendingActivateFlowId = null
                pendingActivateNeedsCamera = false
            }
        } else {
            pendingActivateFlowId = null
            pendingActivateNeedsCamera = false
        }
    }

    fun onToggleChecked(graph: com.devfest.runtime.model.FlowGraph, checked: Boolean) {
        if (!checked) {
            viewModel.toggleFlow(graph.id, false)
            return
        }
        
        val needsDeviceAdmin = FlowPermissionHelper.requiresDeviceAdmin(graph)
        val needsCamera = FlowPermissionHelper.requiresCamera(graph)
        val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        
        // If flow needs device admin but we can't request it (no Activity), don't activate
        if (needsDeviceAdmin && activity == null) {
            // Could show a Snackbar here: "Cannot activate: needs device admin"
            return
        }
        
        // If flow doesn't need device admin, check camera and activate
        if (!needsDeviceAdmin) {
            if (needsCamera && !hasCamera) {
                if (activity != null) {
                    pendingActivateFlowId = graph.id
                    pendingActivateNeedsCamera = true
                    cameraLauncher.launch(Manifest.permission.CAMERA)
                }
                // No Activity to request camera - don't activate a flow that can't run
                return
            }
            viewModel.toggleFlow(graph.id, true)
            return
        }
        
        // Flow needs device admin - check if enabled
        if (!DeviceAdminHelper.isDeviceAdminEnabled(context)) {
            pendingActivateFlowId = graph.id
            pendingActivateNeedsCamera = needsCamera && !hasCamera
            deviceAdminLauncher.launch(DeviceAdminHelper.createAddDeviceAdminIntent(context))
            return
        }
        
        // Device admin already enabled - check camera
        if (needsCamera && !hasCamera && activity != null) {
            pendingActivateFlowId = graph.id
            pendingActivateNeedsCamera = true
            cameraLauncher.launch(Manifest.permission.CAMERA)
        } else {
            viewModel.toggleFlow(graph.id, true)
        }
    }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (uiState.flowGraphs.isEmpty()) {
            // Empty state
            Text(
                text = "No flows created yet. Chat with the agent to start!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            uiState.flowGraphs.values.reversed().forEach { graph -> // Show newest first
                val isActive = uiState.activeFlowIds.contains(graph.id)
                FlowStatusCard(
                    title = graph.title,
                    status = if (isActive) "Active" else "Inactive",
                    statusColor = if (isActive) ActionGreen else Color.Gray,
                    icon = Icons.Filled.Bolt,
                    description = graph.explanation,
                    iconBg = if (isActive) Color(0xFFE0E7FF) else Color.LightGray,
                    iconTint = if (isActive) Color(0xFF6366F1) else Color.DarkGray,
                    isChecked = isActive,
                    onToggle = { checked -> onToggleChecked(graph, checked) },
                    showRunButton = graph.blocks.any { it.type == BlockType.MANUAL_QUICK_TRIGGER },
                    onRun = { viewModel.runFlow(graph) }
                )
            }
        }
    }
}

@Composable
fun FlowStatusCard(
    title: String,
    status: String,
    statusColor: Color,
    icon: ImageVector,
    description: String,
    iconBg: Color,
    iconTint: Color,
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit,
    showRunButton: Boolean = false,
    onRun: () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(iconBg, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = iconTint)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = status,
                                style = MaterialTheme.typography.bodySmall,
                                color = statusColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showRunButton) {
                        androidx.compose.material3.IconButton(
                            onClick = onRun,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.PlayArrow,
                                contentDescription = "Run Now",
                                tint = ElectricBlue
                            )
                        }
                    }
                    Switch(
                        checked = isChecked,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ElectricBlue,
                            uncheckedThumbColor = Color.LightGray,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RecentActivityLog() {
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .padding(start = 8.dp) // Indent for timeline line
    ) {
        TimelineItem(
            time = "Today, 17:30",
            title = "Living Room AC turned on",
            subtitle = "Triggered by: Proximity",
            dotColor = ElectricBlue
        )
        TimelineItem(
            time = "Today, 14:15",
            title = "Daily summary emailed",
            subtitle = "Sent to user@example.com",
            dotColor = Color.Gray
        )
    }
}

@Composable
fun TimelineItem(
    time: String,
    title: String,
    subtitle: String,
    dotColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = time,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private val screenPadding = 24.dp
