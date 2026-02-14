package com.devfest.agentclient

import android.content.Context
import com.devfest.agentclient.model.FlowGraphResponse
import com.devfest.agentclient.model.IntentContext
import com.devfest.agentclient.model.IntentRequest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType

class AgentClient(
    private val api: AgentApi
) {

    suspend fun resolveIntent(
        intentText: String,
        context: IntentContext,
        sessionToken: String
    ): FlowGraphResponse = withContext(Dispatchers.IO) {
        val request = IntentRequest(
            userId = UUID.randomUUID().toString(),
            intentText = intentText,
            context = context,
            sessionToken = sessionToken
        )
        api.resolveIntent(request)
    }

    companion object {
        fun build(
            context: Context,
            baseUrl: String,
            json: Json = defaultJson
        ): AgentClient {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .callTimeout(30.seconds)
                .addInterceptor(logging)
                .addInterceptor { chain ->
                    val request = chain.request()
                    val sessionToken = SessionTokenStore(context).getToken()
                    val newRequest = request.newBuilder()
                        .addHeader("Authorization", "Bearer $sessionToken")
                        .build()
                    chain.proceed(newRequest)
                }
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()

            return AgentClient(retrofit.create(AgentApi::class.java))
        }

        private val defaultJson = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = false
        }
    }
}
