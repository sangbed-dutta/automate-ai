package com.devfest.server.service

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

object JsonParser {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun evaluate(raw: String): JsonElement {
        val trimmed = raw.trim()
        return try {
            json.parseToJsonElement(trimmed)
        } catch (_: Exception) {
            val start = trimmed.indexOf('{')
            val end = trimmed.lastIndexOf('}')
            require(start >= 0 && end > start) { "Unable to locate JSON object in response" }
            json.parseToJsonElement(trimmed.substring(start, end + 1))
        }
    }
}
