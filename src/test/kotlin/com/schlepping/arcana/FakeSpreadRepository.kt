package com.schlepping.arcana

import com.schlepping.arcana.spread.Reading
import com.schlepping.arcana.spread.SpreadRepository
import com.schlepping.arcana.spread.SpreadType
import java.util.UUID

class FakeSpreadRepository : SpreadRepository {

    private val readings = mutableListOf<Reading>()
    private val completedDevices = mutableSetOf<UUID>()
    private val dailySpreads = mutableMapOf<UUID, Int>()

    override suspend fun saveReading(reading: Reading) {
        readings.add(reading)
    }

    override suspend fun findReadingById(readingId: UUID, deviceId: UUID): Reading? =
        readings.find {
            it.id == readingId && it.deviceId == deviceId && it.spreadType != SpreadType.DAILY_CARD
        }

    override suspend fun findReadingsByDevice(
        deviceId: UUID,
        limit: Int,
        offset: Int,
    ): List<Reading> = readings
        .filter { it.deviceId == deviceId && it.spreadType != SpreadType.DAILY_CARD }
        .sortedByDescending { it.createdAt }
        .drop(offset)
        .take(limit)

    override suspend fun hasCompletedSpread(deviceId: UUID): Boolean =
        deviceId in completedDevices

    override suspend fun markSpreadCompleted(deviceId: UUID) {
        completedDevices.add(deviceId)
    }

    override suspend fun tryIncrementSpreadsCount(deviceId: UUID, maxPerDay: Int): Boolean {
        val current = dailySpreads[deviceId] ?: 0
        return if (current >= maxPerDay) {
            false
        } else {
            dailySpreads[deviceId] = current + 1
            true
        }
    }

    override suspend fun decrementSpreadsCount(deviceId: UUID, date: java.time.LocalDate) {
        val current = dailySpreads[deviceId] ?: return
        dailySpreads[deviceId] = (current - 1).coerceAtLeast(0)
    }

    // Test helpers
    fun readingCount(): Int = readings.size
    fun allReadings(): List<Reading> = readings.toList()

    fun setTodaySpreads(deviceId: UUID, count: Int) {
        dailySpreads[deviceId] = count
    }

    fun markCompleted(deviceId: UUID) {
        completedDevices.add(deviceId)
    }

    fun todaySpreads(deviceId: UUID): Int = dailySpreads[deviceId] ?: 0

    fun clear() {
        readings.clear()
        completedDevices.clear()
        dailySpreads.clear()
    }
}
