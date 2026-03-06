package com.schlepping.arcana.spread

import com.schlepping.arcana.auth.deviceId
import com.schlepping.arcana.auth.userTier
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.spreadRoutes(service: SpreadService) {
    authenticate("auth-jwt") {
        route("/api/v1/readings") {
            post {
                val principal = call.principal<JWTPrincipal>()!!
                val deviceId = principal.deviceId()
                val tier = principal.userTier()

                val request = call.receive<CreateReadingRequest>()
                val response = service.createReading(deviceId, request, tier)

                call.respond(HttpStatusCode.Created, response)
            }

            get {
                val principal = call.principal<JWTPrincipal>()!!
                val deviceId = principal.deviceId()

                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

                val response = service.listReadings(deviceId, limit, offset)
                call.respond(HttpStatusCode.OK, response)
            }

            get("/{readingId}") {
                val principal = call.principal<JWTPrincipal>()!!
                val deviceId = principal.deviceId()

                val readingId = try {
                    UUID.fromString(call.parameters["readingId"])
                } catch (_: IllegalArgumentException) {
                    throw IllegalArgumentException("Invalid reading ID format")
                }

                val response = service.getReading(deviceId, readingId)
                call.respond(HttpStatusCode.OK, response)
            }
        }
    }
}
