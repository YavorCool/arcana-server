package com.schlepping.arcana

import com.schlepping.arcana.auth.AuthService
import com.schlepping.arcana.auth.authRoutes
import com.schlepping.arcana.daily.DailyCardService
import com.schlepping.arcana.daily.dailyCardRoutes
import com.schlepping.arcana.spread.SpreadService
import com.schlepping.arcana.spread.spreadRoutes
import io.ktor.server.application.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val authService by inject<AuthService>()
    val dailyCardService by inject<DailyCardService>()
    val spreadService by inject<SpreadService>()

    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }

        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")

        authRoutes(authService)
        dailyCardRoutes(dailyCardService)
        spreadRoutes(spreadService)
    }
}
