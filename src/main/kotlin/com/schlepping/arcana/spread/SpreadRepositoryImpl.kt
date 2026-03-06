package com.schlepping.arcana.spread

import com.schlepping.arcana.db.DailyUsage
import com.schlepping.arcana.db.Devices
import com.schlepping.arcana.db.Readings
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class SpreadRepositoryImpl : SpreadRepository {

    override suspend fun saveReading(reading: Reading): Unit = newSuspendedTransaction {
        Readings.insert {
            it[id] = reading.id
            it[deviceId] = reading.deviceId
            it[spreadType] = reading.spreadType.value
            it[question] = reading.question
            it[cards] = Json.encodeToString(reading.cards)
            it[interpretation] = reading.interpretation
            it[createdAt] = reading.createdAt
        }
    }

    override suspend fun findReadingById(readingId: UUID, deviceId: UUID): Reading? =
        newSuspendedTransaction {
            Readings.selectAll().where {
                (Readings.id eq readingId) and
                    (Readings.deviceId eq deviceId) and
                    (Readings.spreadType neq SpreadType.DAILY_CARD.value)
            }.firstOrNull()?.toReading()
        }

    override suspend fun findReadingsByDevice(
        deviceId: UUID,
        limit: Int,
        offset: Int,
    ): List<Reading> = newSuspendedTransaction {
        Readings.selectAll()
            .where {
                (Readings.deviceId eq deviceId) and
                    (Readings.spreadType neq SpreadType.DAILY_CARD.value)
            }
            .orderBy(Readings.createdAt, SortOrder.DESC)
            .limit(limit)
            .offset(offset.toLong())
            .map { it.toReading() }
    }

    override suspend fun hasCompletedSpread(deviceId: UUID): Boolean = newSuspendedTransaction {
        Devices.selectAll().where { Devices.deviceId eq deviceId }
            .firstOrNull()?.get(Devices.hasCompletedSpread) ?: false
    }

    override suspend fun markSpreadCompleted(deviceId: UUID): Unit = newSuspendedTransaction {
        Devices.update({ Devices.deviceId eq deviceId }) {
            it[hasCompletedSpread] = true
        }
    }

    override suspend fun tryIncrementSpreadsCount(deviceId: UUID, maxPerDay: Int): Boolean =
        newSuspendedTransaction {
            val today = LocalDate.now(ZoneOffset.UTC)

            // Upsert: insert with count=1, or increment if exists and below limit
            val existing = DailyUsage.selectAll().where {
                (DailyUsage.deviceId eq deviceId) and (DailyUsage.date eq today)
            }.forUpdate().firstOrNull()

            if (existing == null) {
                try {
                    DailyUsage.insert {
                        it[DailyUsage.deviceId] = deviceId
                        it[date] = today
                        it[spreadsCount] = 1
                    }
                    true
                } catch (_: org.jetbrains.exposed.exceptions.ExposedSQLException) {
                    // Concurrent insert — re-read and try increment
                    val row = DailyUsage.selectAll().where {
                        (DailyUsage.deviceId eq deviceId) and (DailyUsage.date eq today)
                    }.forUpdate().first()
                    val count = row[DailyUsage.spreadsCount]
                    if (count >= maxPerDay) false
                    else {
                        DailyUsage.update({
                            (DailyUsage.deviceId eq deviceId) and (DailyUsage.date eq today)
                        }) { it[spreadsCount] = count + 1 }
                        true
                    }
                }
            } else {
                val currentCount = existing[DailyUsage.spreadsCount]
                if (currentCount >= maxPerDay) {
                    false
                } else {
                    DailyUsage.update({
                        (DailyUsage.deviceId eq deviceId) and (DailyUsage.date eq today)
                    }) {
                        it[spreadsCount] = currentCount + 1
                    }
                    true
                }
            }
        }

    override suspend fun decrementSpreadsCount(deviceId: UUID, date: LocalDate): Unit =
        newSuspendedTransaction {
            DailyUsage.update({
                (DailyUsage.deviceId eq deviceId) and (DailyUsage.date eq date)
        }) {
            with(SqlExpressionBuilder) {
                it[spreadsCount] = spreadsCount - 1
            }
        }
        }

    private fun ResultRow.toReading() = Reading(
        id = this[Readings.id],
        deviceId = this[Readings.deviceId],
        spreadType = SpreadType.fromString(this[Readings.spreadType])
            ?: error("Unknown spread type in DB: ${this[Readings.spreadType]}"),
        question = this[Readings.question],
        cards = Json.decodeFromString<List<CardData>>(this[Readings.cards]),
        interpretation = this[Readings.interpretation],
        createdAt = this[Readings.createdAt],
    )
}
