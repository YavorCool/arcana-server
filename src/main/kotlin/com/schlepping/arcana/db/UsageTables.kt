package com.schlepping.arcana.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime

object DailyUsage : Table("daily_usage") {
    val deviceId = uuid("device_id").references(Devices.deviceId)
    val date = date("date")
    val readingsCount = integer("readings_count").default(0)
    val chatQuestionsCount = integer("chat_questions_count").default(0)

    override val primaryKey = PrimaryKey(deviceId, date)
}