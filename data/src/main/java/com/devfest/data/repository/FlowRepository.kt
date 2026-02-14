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
    fun observeFlows(): Flow<List<FlowGraph>>
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
        return response.graph
    }

    override suspend fun getFlow(id: String): FlowGraph? =
        flowDao.getById(id)?.let { entity ->
            json.decodeFromString(FlowGraph.serializer(), entity.graphJson)
        }

    override fun observeFlows(): Flow<List<FlowGraph>> =
        flowDao.observeAll().map { entities ->
            entities.map { entity ->
                json.decodeFromString(FlowGraph.serializer(), entity.graphJson)
            }
        }

    private suspend fun persist(response: FlowGraphResponse) {
        val entity = FlowEntity(
            id = response.flowId,
            title = response.graph.title,
            graphJson = json.encodeToString(FlowGraph.serializer(), response.graph),
            explanation = response.explanation,
            riskFlags = response.riskFlags.joinToString(separator = ",")
        )
        flowDao.upsert(entity)
    }
}
