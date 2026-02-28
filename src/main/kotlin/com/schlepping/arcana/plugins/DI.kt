package com.schlepping.arcana.plugins

import com.schlepping.arcana.auth.authModule
import com.schlepping.arcana.daily.dailyCardModule
import com.schlepping.arcana.llm.llmModule
import io.ktor.server.application.*
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureDI() {
    install(Koin) {
        slf4jLogger()
        modules(
            authModule(),
            llmModule(),
            dailyCardModule(),
        )
    }
}
