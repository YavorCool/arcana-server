package com.schlepping.arcana.plugins

import io.ktor.server.application.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

fun Application.configureDatabase() {
    val dbHost = System.getenv("DB_HOST") ?: "localhost"
    val dbPort = System.getenv("DB_PORT") ?: "5435"
    val dbName = System.getenv("DB_NAME") ?: "arcana"
    val user = System.getenv("DB_USER") ?: environment.config.property("database.user").getString()
    val password = System.getenv("DB_PASSWORD") ?: environment.config.property("database.password").getString()
    val dbParams = System.getenv("DB_PARAMS") ?: ""
    val url = "jdbc:postgresql://$dbHost:$dbPort/$dbName${if (dbParams.isNotEmpty()) "?$dbParams" else ""}"

    Flyway.configure()
        .dataSource(url, user, password)
        .baselineOnMigrate(true)
        .load()
        .migrate()

    Database.connect(
        url = url,
        driver = "org.postgresql.Driver",
        user = user,
        password = password,
    )
}