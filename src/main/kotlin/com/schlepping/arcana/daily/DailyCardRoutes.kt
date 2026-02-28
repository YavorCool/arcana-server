package com.schlepping.arcana.daily

import com.schlepping.arcana.auth.deviceId
import com.schlepping.arcana.auth.userTier
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.dailyCardRoutes(service: DailyCardService) {
    authenticate("auth-jwt") {
        route("/api/v1/readings") {
            post("/daily") {
                val principal = call.principal<JWTPrincipal>()!!
                val deviceId = principal.deviceId()
                val tier = principal.userTier()

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
