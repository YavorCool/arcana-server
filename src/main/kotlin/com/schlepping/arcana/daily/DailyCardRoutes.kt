package com.schlepping.arcana.daily

import com.schlepping.arcana.llm.UserTier
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.dailyCardRoutes(service: DailyCardService) {
    authenticate("auth-jwt") {
        route("/api/v1/readings") {
            post("/daily") {
                val principal = call.principal<JWTPrincipal>()!!
                val deviceId = UUID.fromString(principal.payload.getClaim("deviceId").asString())
                val tierClaim = principal.payload.getClaim("tier")?.asString() ?: "free"
                val tier = when (tierClaim.lowercase()) {
                    "premium" -> UserTier.PREMIUM
                    else -> UserTier.FREE
                }

                val request = call.receive<DailyCardRequest>()
                val response = service.getDailyCard(
                    deviceId = deviceId,
                    cardName = request.cardName,
                    isReversed = request.isReversed,
                    tier = tier,
                    querentName = request.querentName,
                )

                call.respond(HttpStatusCode.OK, response)
            }
        }
    }
}
