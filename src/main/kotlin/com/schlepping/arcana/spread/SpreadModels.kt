package com.schlepping.arcana.spread

import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

enum class SpreadType(val value: String, val requiredCardCount: Int) {
    YES_NO("yes_no", 1),
    PAST_PRESENT_FUTURE("past_present_future", 3),
    QUESTION_CARD("question_card", 1),
    DAILY_CARD("daily_card", 1);

    companion object {
        private val byValue = entries.associateBy { it.value }
        fun fromString(s: String): SpreadType? = byValue[s.lowercase()]
    }
}

@Serializable
data class CardData(
    val cardName: String,
    val isReversed: Boolean,
)

@Serializable
data class CreateReadingRequest(
    val spreadType: String,
    val question: String? = null,
    val cards: List<CardData>,
    val querentName: String? = null,
)

@Serializable
data class CreateReadingResponse(
    val readingId: String,
    val interpretation: String,
)

@Serializable
data class ReadingSummary(
    val readingId: String,
    val spreadType: String,
    val question: String?,
    val cards: List<CardData>,
    val createdAt: String,
)

@Serializable
data class ReadingDetail(
    val readingId: String,
    val spreadType: String,
    val question: String?,
    val cards: List<CardData>,
    val interpretation: String,
    val createdAt: String,
)

@Serializable
data class ReadingsListResponse(
    val readings: List<ReadingSummary>,
    val hasMore: Boolean,
)

data class Reading(
    val id: UUID,
    val deviceId: UUID,
    val spreadType: SpreadType,
    val question: String?,
    val cards: List<CardData>,
    val interpretation: String?,
    val createdAt: LocalDateTime,
)
