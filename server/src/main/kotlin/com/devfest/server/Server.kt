package com.devfest.server

import com.devfest.server.model.FlowGraphResponse
import com.devfest.server.model.IntentRequest
import com.devfest.server.service.FlowSynthesisService
import com.devfest.server.validation.FlowValidator
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

fun main(args: Array<String>) = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused")
fun Application.module() {
    val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }

    install(CallLogging) {
        level = Level.INFO
    }
    install(ContentNegotiation) {
        json(json)
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to (cause.localizedMessage ?: "unknown error"))
            )
        }
    }
    installAuthentication()

    val openAiKey = environment.config.propertyOrNull("openai.key")?.getString()
        ?: System.getenv("OPENAI_API_KEY")
    val flowValidator = FlowValidator()
    val synthesisService = FlowSynthesisService(
        openAiKey = openAiKey,
        json = json,
        validator = flowValidator
    )

    routing {
        authenticate(optional = true) {
            post("/intents/resolve") {
                val request = call.receive<IntentRequest>()
                val response: FlowGraphResponse = synthesisService.synthesize(request)
                call.respond(response)
            }
        }
    }
}

private fun Application.installAuthentication() {
    install(io.ktor.server.auth.Authentication) {
        jwt {
            verifier(FakeJwtProvider.verifier)
            validate { credential ->
                if (credential.payload.getClaim("sub").asString().isNullOrEmpty()) null else credential
            }
        }
    }
}
