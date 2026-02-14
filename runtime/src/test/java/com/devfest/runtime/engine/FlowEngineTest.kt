package com.devfest.runtime.engine

import com.devfest.runtime.engine.handlers.TriggerHandler
import com.devfest.runtime.model.BlockType
import com.devfest.runtime.model.FlowBlock
import com.devfest.runtime.model.FlowEdge
import com.devfest.runtime.model.FlowGraph
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FlowEngineTest {

    @Test
    fun `executes simple trigger chain`() = runTest {
        val graph = FlowGraph(
            id = "flow",
            title = "Test",
            blocks = listOf(
                FlowBlock("trigger", BlockType.MANUAL_QUICK_TRIGGER, emptyMap()),
                FlowBlock("action", BlockType.PLAY_SOUND_ACTION, emptyMap())
            ),
            edges = listOf(FlowEdge("trigger", "action", "always")),
            explanation = "Test flow",
            riskFlags = emptyList()
        )

        val engine = FlowEngine(
            handlers = mapOf(
                BlockType.MANUAL_QUICK_TRIGGER to TriggerHandler("manual trigger"),
                BlockType.PLAY_SOUND_ACTION to TriggerHandler("simulated action")
            )
        )

        val result = engine.execute(graph, FlowExecutionInput())

        assertEquals(2, result.steps.size)
        assertEquals("trigger", result.steps.first().blockId)
        assertEquals("action", result.steps.last().blockId)
    }
}
