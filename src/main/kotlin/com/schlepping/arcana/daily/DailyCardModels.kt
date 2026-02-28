package com.schlepping.arcana.daily

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class DailyCardRequest(
    val cardName: String,
    val isReversed: Boolean,
    val querentName: String? = null,
)

@Serializable
data class DailyCardResponse(
    val readingId: String,
    val cardName: String,
    val isReversed: Boolean,
    val interpretation: String,
    val cached: Boolean,
)

data class DailyCardReading(
    val id: UUID,
    val deviceId: UUID,
    val cardName: String,
    val isReversed: Boolean,
    val interpretation: String,
    val createdAt: LocalDateTime,
)

@Serializable
data class CardData(
    val cardName: String,
    val isReversed: Boolean,
)
