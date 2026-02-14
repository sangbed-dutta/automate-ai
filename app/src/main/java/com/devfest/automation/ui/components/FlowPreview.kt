package com.devfest.automation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FlowPreview(
    title: String,
    explanation: String,
    blocks: List<Pair<String, Map<String, String>>>
) {
    Column {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = explanation, style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(8.dp))
        blocks.forEach { (type, params) ->
            Text(
                text = "• $type ${if (params.isNotEmpty()) params else ""}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
