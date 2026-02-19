package com.schlepping.arcana.plugins

import com.schlepping.arcana.db.ChatMessages
import com.schlepping.arcana.db.DailyUsage
import com.schlepping.arcana.db.Devices
import com.schlepping.arcana.db.Readings
import com.schlepping.arcana.db.RefreshTokens
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabase() {
    val dbHost = System.getenv("DB_HOST") ?: "localhost"
    val dbPort = System.getenv("DB_PORT") ?: "5435"
    val dbName = System.getenv("DB_NAME") ?: "arcana"
    val user = System.getenv("DB_USER") ?: environment.config.property("database.user").getString()
    val password = System.getenv("DB_PASSWORD") ?: environment.config.property("database.password").getString()
    val url = "jdbc:postgresql://$dbHost:$dbPort/$dbName"

    Database.connect(
        url = url,
        driver = "org.postgresql.Driver",
        user = user,
        password = password,
    )

    transaction {
        SchemaUtils.create(
            Devices,
            RefreshTokens,
            Readings,
            ChatMessages,
            DailyUsage,
        )
    }
}