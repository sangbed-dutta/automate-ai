package com.devfest.runtime.engine.blocks

import android.content.Context
import com.devfest.runtime.engine.FlowBlockHandler
import com.devfest.runtime.engine.FlowExecutionInput
import com.devfest.runtime.engine.FlowExecutionState
import com.devfest.runtime.engine.FlowStepResult
import com.devfest.runtime.engine.FlowStepStatus
import com.devfest.runtime.model.FlowBlock

class BatteryLevelHandler(context: Context) : FlowBlockHandler {
    
    private val condition = BatteryLevelCondition(context)

    override suspend fun handle(
        block: FlowBlock,
        input: FlowExecutionInput,
        state: FlowExecutionState
    ): FlowStepResult {
        val min = block.params["minLevel"]?.toIntOrNull() ?: 0
        val max = block.params["maxLevel"]?.toIntOrNull() ?: 100
        
        val success = condition.check(min, max)
        
        return FlowStepResult(
            blockId = block.id,
            status = if (success) FlowStepStatus.SUCCESS else FlowStepStatus.SKIPPED,
            message = if (success) "Battery within range $min-$max" else "Battery outside range $min-$max"
        )
    }
}
