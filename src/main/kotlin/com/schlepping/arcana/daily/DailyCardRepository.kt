package com.schlepping.arcana.daily

import com.schlepping.arcana.db.Readings
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

interface DailyCardRepository {
    suspend fun findTodaysDailyCard(deviceId: UUID): DailyCardReading?
    suspend fun saveDailyCard(
        id: UUID,
        deviceId: UUID,
        cardName: String,
        isReversed: Boolean,
        interpretation: String,
    )
}

class DailyCardRepositoryImpl : DailyCardRepository {

    override suspend fun findTodaysDailyCard(deviceId: UUID): DailyCardReading? = newSuspendedTransaction {
        val today = LocalDate.now(ZoneOffset.UTC)
        val startOfDay = today.atStartOfDay()
        val endOfDay = today.plusDays(1).atStartOfDay()

        Readings.selectAll().where {
            (Readings.deviceId eq deviceId) and
                (Readings.spreadType eq "daily_card") and
                (Readings.createdAt greaterEq startOfDay) and
                (Readings.createdAt less endOfDay)
        }.firstOrNull()?.let { row ->
            val cardsJson = row[Readings.cards]
            val cardData = Json.decodeFromString<CardData>(cardsJson)

            DailyCardReading(
                id = row[Readings.id],
                deviceId = row[Readings.deviceId],
                cardName = cardData.cardName,
                isReversed = cardData.isReversed,
                interpretation = row[Readings.interpretation] ?: "",
                createdAt = row[Readings.createdAt],
            )
        }
    }

    override suspend fun saveDailyCard(
        id: UUID,
        deviceId: UUID,
        cardName: String,
        isReversed: Boolean,
        interpretation: String,
    ): Unit = newSuspendedTransaction {
        Readings.insert {
            it[Readings.id] = id
            it[Readings.deviceId] = deviceId
            it[Readings.spreadType] = "daily_card"
            it[Readings.cards] = Json.encodeToString(CardData(cardName, isReversed))
            it[Readings.interpretation] = interpretation
        }
    }
}
