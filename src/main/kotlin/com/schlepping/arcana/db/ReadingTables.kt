package com.schlepping.arcana.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object Readings : Table("readings") {
    val id = uuid("id")
    val deviceId = uuid("device_id").references(Devices.deviceId)
    val spreadType = varchar("spread_type", 50)
    val question = varchar("question", 500).nullable()
    val cards = text("cards")
    val interpretation = text("interpretation").nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(id)
}

object ChatMessages : Table("chat_messages") {
    val id = uuid("id")
    val readingId = uuid("reading_id").references(Readings.id)
    val role = varchar("role", 10)
    val text = text("text")
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(id)
}