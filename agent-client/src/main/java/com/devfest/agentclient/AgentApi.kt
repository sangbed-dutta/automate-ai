package com.devfest.agentclient

import com.devfest.agentclient.model.FlowGraphResponse
import com.devfest.agentclient.model.IntentRequest
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface AgentApi {
    @Headers("Content-Type: application/json")
    @POST("/intents/resolve")
    suspend fun resolveIntent(@Body request: IntentRequest): FlowGraphResponse
}
