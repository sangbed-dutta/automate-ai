package com.devfest.server

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm

object FakeJwtProvider {
    private val algorithm = Algorithm.HMAC256(System.getenv("JWT_SECRET") ?: "devfest-secret")

    val verifier = JWT.require(algorithm).build()
}
