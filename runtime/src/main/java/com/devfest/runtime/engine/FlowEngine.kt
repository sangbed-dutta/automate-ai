package com.devfest.runtime.engine

import com.devfest.runtime.model.BlockCategory
import com.devfest.runtime.model.BlockType
import com.devfest.runtime.model.FlowBlock
import com.devfest.runtime.model.FlowEdge
import com.devfest.runtime.model.FlowGraph
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FlowEngine(
    private val handlers: Map<BlockType, FlowBlockHandler>,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun execute(
        graph: FlowGraph,
        input: FlowExecutionInput
    ): FlowExecutionResult = withContext(dispatcher) {
        val state = FlowExecutionState(mutableMapOf())
        val edgesBySource = graph.edges.groupBy(FlowEdge::from)

        val triggerBlocks = graph.blocks.filter { it.type.category == BlockCategory.TRIGGER }
        val visited = mutableSetOf<String>()
        val results = mutableListOf<FlowStepResult>()

        val startNodes = if (triggerBlocks.isNotEmpty()) triggerBlocks else graph.blocks.take(1)

        for (block in startNodes) {
            traverse(block, edgesBySource, graph.blocks.associateBy(FlowBlock::id), input, state, results, visited)
        }

        FlowExecutionResult(results)
    }

    private suspend fun traverse(
        block: FlowBlock,
        edgesBySource: Map<String, List<FlowEdge>>,
        blocksById: Map<String, FlowBlock>,
        input: FlowExecutionInput,
        state: FlowExecutionState,
        results: MutableList<FlowStepResult>,
        visited: MutableSet<String>
    ) {
        if (!visited.add(block.id)) return
        val handler = handlers[block.type] ?: return
        val outcome = handler.handle(block, input, state)
        results += outcome

        val nextEdges = edgesBySource[block.id].orEmpty()
        for (edge in nextEdges) {
            val next = blocksById[edge.to] ?: continue
            traverse(next, edgesBySource, blocksById, input, state, results, visited)
        }
    }
}

data class FlowExecutionInput(
    val metadata: Map<String, String> = emptyMap()
)

data class FlowExecutionState(
    val variables: MutableMap<String, String>
)

data class FlowStepResult(
    val blockId: String,
    val status: FlowStepStatus,
    val message: String = ""
)

enum class FlowStepStatus { SUCCESS, SKIPPED, FAILED }

data class FlowExecutionResult(
    val steps: List<FlowStepResult>
)

interface FlowBlockHandler {
    suspend fun handle(
        block: FlowBlock,
        input: FlowExecutionInput,
        state: FlowExecutionState
    ): FlowStepResult
}
