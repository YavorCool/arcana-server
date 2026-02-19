package com.schlepping.arcana

import com.schlepping.arcana.plugins.configureDI
import com.schlepping.arcana.plugins.configureDatabase
import com.schlepping.arcana.plugins.configureMonitoring
import com.schlepping.arcana.plugins.configureSecurity
import com.schlepping.arcana.plugins.configureSerialization
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configureDI()
    configureDatabase()
    configureSerialization()
    configureMonitoring()
    configureSecurity()
    configureRouting()
}
