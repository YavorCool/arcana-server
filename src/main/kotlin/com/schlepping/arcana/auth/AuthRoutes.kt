package com.schlepping.arcana.auth

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.authRoutes(authService: AuthService) {
    route("/api/v1/auth") {
        post("register") {
            val request = call.receive<RegisterRequest>()
            val deviceId = UUID.fromString(request.deviceId)
            val response = authService.register(deviceId, request.platform)
            call.respond(HttpStatusCode.Created, response)
        }

    post("/refresh") {
            val request = call.receive<RefreshRequest>()
            val response = authService.refresh(request.refreshToken)
            call.respond(response)
        }
    }
}