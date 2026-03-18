package com.schlepping.arcana.chat

import com.schlepping.arcana.auth.deviceId
import com.schlepping.arcana.auth.userTier
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.chatRoutes(service: ChatService) {
    authenticate("auth-jwt") {
        route("/api/v1/readings/{readingId}/chat") {
            post { sendMessage(service) }
        }
    }
}

private suspend fun RoutingContext.sendMessage(service: ChatService) {
    val principal = call.principal<JWTPrincipal>()!!
    val deviceId = principal.deviceId()
    val tier = principal.userTier()

    val readingId = try {
        UUID.fromString(call.parameters["readingId"])
    } catch (_: IllegalArgumentException) {
        throw IllegalArgumentException("Invalid reading ID format")
    }

    val request = call.receive<ChatMessageRequest>()
    val response = service.sendMessage(deviceId, readingId, request.message, tier)

    call.respond(HttpStatusCode.Created, response)
}
