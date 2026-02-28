package com.schlepping.arcana.db

import com.schlepping.arcana.user.UserTier
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object Devices : Table("devices") {
    val deviceId = uuid("device_id")
    val platform = varchar("platform", 10)
    val attested = bool("attested").default(true)
    val attestedAt = datetime("attested_at").nullable()
    val tier = varchar("tier", 20).default(UserTier.FREE.value)
    val paymentId = varchar("payment_id", 255).nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val lastSeenAt = datetime("last_seen_at").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(deviceId)
}

object RefreshTokens : Table("refresh_tokens") {
    val token = varchar("token", 255)
    val deviceId = uuid("device_id").references(Devices.deviceId)
    val expiresAt = datetime("expires_at")
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(token)
}