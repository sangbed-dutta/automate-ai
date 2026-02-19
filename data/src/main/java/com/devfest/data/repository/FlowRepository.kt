package com.devfest.data.repository

import com.devfest.agentclient.AgentClient
import com.devfest.agentclient.model.FlowGraphResponse
import com.devfest.agentclient.model.IntentContext
import com.devfest.data.local.FlowDao
import com.devfest.data.local.FlowEntity
import com.devfest.runtime.model.FlowGraph
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface FlowRepository {
    suspend fun requestFlow(intentText: String, context: IntentContext, sessionToken: String): FlowGraph
    suspend fun getFlow(id: String): FlowGraph?
    fun observeFlows(): Flow<List<Pair<FlowGraph, Boolean>>>
    suspend fun setActive(id: String, active: Boolean)
    /** Returns all active (and published) flow graphs. Used e.g. by pattern-failure service. */
    suspend fun getActiveFlowGraphs(): List<FlowGraph>
}

class DefaultFlowRepository(
    private val agentClient: AgentClient,
    private val flowDao: FlowDao,
    private val json: Json
) : FlowRepository {

    override suspend fun requestFlow(
        intentText: String,
        context: IntentContext,
        sessionToken: String
    ): FlowGraph {
        val response = agentClient.resolveIntent(intentText, context, sessionToken)
        persist(response)
        // Return graph with the CORRECT ID (Entity ID), matching what observeFlows returns
        return response.graph.copy(id = response.flowId)
    }

    override suspend fun getFlow(id: String): FlowGraph? =
        flowDao.getById(id)?.let { entity ->
            json.decodeFromString(FlowGraph.serializer(), entity.graphJson).copy(id = entity.id)
        }

    override fun observeFlows(): Flow<List<Pair<FlowGraph, Boolean>>> =
        flowDao.observeAll().map { entities ->
            entities.filter { !it.isDraft } // ONLY show non-draft flows in global list (Dashboard)
                .map { entity ->
                    val graph = json.decodeFromString(FlowGraph.serializer(), entity.graphJson)
                    // Force ID consistency: The DB ID is the source of truth
                    graph.copy(id = entity.id) to entity.isActive
                }
        }

    override suspend fun setActive(id: String, active: Boolean) {
        // When setting active, we also implicitly publish (remove draft status)
        flowDao.updateStatus(id, active, isDraft = false)
    }

    override suspend fun getActiveFlowGraphs(): List<FlowGraph> =
        flowDao.getActiveFlows().map { entity ->
            json.decodeFromString(FlowGraph.serializer(), entity.graphJson).copy(id = entity.id)
        }

    private suspend fun persist(response: FlowGraphResponse) {
        val entity = FlowEntity(
            id = response.flowId,
            title = response.graph.title,
            graphJson = json.encodeToString(FlowGraph.serializer(), response.graph),
            explanation = response.explanation,
            riskFlags = response.riskFlags.joinToString(separator = ","),
            isActive = false,
            isDraft = true // Default to draft
        )
        flowDao.upsert(entity)
    }
}
