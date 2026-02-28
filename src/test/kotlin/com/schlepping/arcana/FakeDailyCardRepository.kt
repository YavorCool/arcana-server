package com.schlepping.arcana

import com.schlepping.arcana.daily.DailyCardReading
import com.schlepping.arcana.daily.DailyCardRepository
import java.time.LocalDateTime
import java.util.UUID

class FakeDailyCardRepository : DailyCardRepository {

    private val store = mutableMapOf<UUID, DailyCardReading>()

    override suspend fun findTodaysDailyCard(deviceId: UUID): DailyCardReading? =
        store[deviceId]

    override suspend fun saveDailyCard(
        id: UUID,
        deviceId: UUID,
        cardName: String,
        isReversed: Boolean,
        interpretation: String,
    ) {
        store[deviceId] = DailyCardReading(
            id = id,
            deviceId = deviceId,
            cardName = cardName,
            isReversed = isReversed,
            interpretation = interpretation,
            createdAt = LocalDateTime.now(),
        )
    }

    fun clear() {
        store.clear()
    }
}
