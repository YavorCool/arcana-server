package com.schlepping.arcana.spread

import java.util.UUID

interface SpreadRepository {
    suspend fun saveReading(reading: Reading)
    suspend fun findReadingById(readingId: UUID, deviceId: UUID): Reading?
    suspend fun findReadingsByDevice(deviceId: UUID, limit: Int, offset: Int): List<Reading>
    suspend fun hasCompletedSpread(deviceId: UUID): Boolean
    suspend fun markSpreadCompleted(deviceId: UUID)

    /**
     * Atomically increments today's spread count for a device, but only if the current count
     * is below [maxPerDay]. Returns `true` if the increment succeeded (limit not reached),
     * `false` if the limit was already reached.
     */
    suspend fun tryIncrementSpreadsCount(deviceId: UUID, maxPerDay: Int): Boolean
    suspend fun decrementSpreadsCount(deviceId: UUID, date: java.time.LocalDate)
}
