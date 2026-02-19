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
    val url = environment.config.property("database.url").getString()
    val user = environment.config.property("database.user").getString()
    val password = environment.config.property("database.password").getString()

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