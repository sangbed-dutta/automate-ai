package com.devfest.automation.util

import com.devfest.runtime.model.BlockType
import com.devfest.runtime.model.FlowGraph

/**
 * Determines what permissions a flow requires based on its blocks.
 * Extensible: add new block types to the lists below to require device admin or other permissions.
 */
object FlowPermissionHelper {

    /**
     * Block types that require device admin permission.
     * Add new block types here as you create flows that need device admin.
     */
    private val DEVICE_ADMIN_REQUIRED_BLOCKS = setOf(
        BlockType.PATTERN_FAILURE_TRIGGER
        // Future: BlockType.DEVICE_LOCK_ACTION, BlockType.WIPE_DATA_ACTION, etc.
    )

    /**
     * Block types that require camera permission.
     * Add new block types here as you create flows that need camera.
     */
    private val CAMERA_REQUIRED_BLOCKS = setOf(
        BlockType.CAMERA
    )

    /**
     * Checks if the flow requires device admin permission.
     * Returns true if any block in the flow is in DEVICE_ADMIN_REQUIRED_BLOCKS.
     */
    fun requiresDeviceAdmin(graph: FlowGraph): Boolean {
        return graph.blocks.any { it.type in DEVICE_ADMIN_REQUIRED_BLOCKS }
    }

    /**
     * Checks if the flow requires camera permission.
     * Returns true if any block in the flow is in CAMERA_REQUIRED_BLOCKS.
     */
    fun requiresCamera(graph: FlowGraph): Boolean {
        return graph.blocks.any { it.type in CAMERA_REQUIRED_BLOCKS }
    }

    /**
     * Returns a list of all block types in the flow that require device admin.
     * Useful for debugging or showing which blocks triggered the requirement.
     */
    fun getDeviceAdminRequiredBlocks(graph: FlowGraph): List<BlockType> {
        return graph.blocks.map { it.type }.filter { it in DEVICE_ADMIN_REQUIRED_BLOCKS }
    }

    /**
     * Checks if the flow is specifically a pattern-failure flow (has PatternFailureTrigger).
     * This is different from requiresDeviceAdmin() - a flow might need device admin for other reasons
     * (e.g. lock device action) but shouldn't run when pattern fails.
     * 
     * Used by PatternFailureFlowService to only run flows that should execute on unlock failure.
     */
    fun hasPatternFailureTrigger(graph: FlowGraph): Boolean {
        return graph.blocks.any { it.type == BlockType.PATTERN_FAILURE_TRIGGER }
    }
}
