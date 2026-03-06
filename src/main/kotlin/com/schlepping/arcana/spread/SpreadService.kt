package com.schlepping.arcana.spread

import com.schlepping.arcana.llm.LlmProvider
import com.schlepping.arcana.llm.RequestType
import com.schlepping.arcana.llm.prompt.PromptBuilder
import com.schlepping.arcana.llm.routing.LlmRouter
import com.schlepping.arcana.user.UserTier
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

class SpreadService(
    private val llmProvider: LlmProvider,
    private val repository: SpreadRepository,
    private val router: LlmRouter,
    private val promptBuilder: PromptBuilder,
) {

    suspend fun createReading(
        deviceId: UUID,
        request: CreateReadingRequest,
        tier: UserTier,
    ): CreateReadingResponse {
        SpreadValidator.validate(request)
        val spreadType = SpreadType.fromString(request.spreadType)!!

        val today = LocalDate.now(ZoneOffset.UTC)

        val maxPerDay = when (tier) {
            UserTier.FREE -> 1
            UserTier.PREMIUM -> Int.MAX_VALUE
        }
        if (!repository.tryIncrementSpreadsCount(deviceId, maxPerDay)) {
            throw SpreadException.DailyLimitReached()
        }

        val isFirstReading = !repository.hasCompletedSpread(deviceId)
        val modelId = router.resolve(tier, RequestType.READING, isFirstReading)

        val prompt = promptBuilder.buildSpreadPrompt(
            spreadType = spreadType,
            cards = request.cards,
            question = request.question,
            querentName = request.querentName,
        ).copy(modelId = modelId)

        try {
            val llmResponse = llmProvider.generate(prompt)

            val readingId = UUID.randomUUID()
            val reading = Reading(
                id = readingId,
                deviceId = deviceId,
                spreadType = spreadType,
                question = request.question,
                cards = request.cards,
                interpretation = llmResponse.content,
                createdAt = LocalDateTime.now(ZoneOffset.UTC),
            )
            repository.saveReading(reading)

            if (isFirstReading) {
                repository.markSpreadCompleted(deviceId)
            }

            return CreateReadingResponse(
                readingId = readingId.toString(),
                interpretation = llmResponse.content,
            )
        } catch (e: Exception) {
            repository.decrementSpreadsCount(deviceId, today)
            throw e
        }
    }

    suspend fun getReading(deviceId: UUID, readingId: UUID): ReadingDetail {
        val reading = repository.findReadingById(readingId, deviceId)
            ?: throw IllegalArgumentException("Reading not found")
        return reading.toDetail()
    }

    suspend fun listReadings(
        deviceId: UUID,
        limit: Int = 20,
        offset: Int = 0,
    ): ReadingsListResponse {
        val clampedLimit = limit.coerceIn(1, 50)
        val clampedOffset = offset.coerceAtLeast(0)
        val readings = repository.findReadingsByDevice(deviceId, clampedLimit + 1, clampedOffset)

        val hasMore = readings.size > clampedLimit
        val page = readings.take(clampedLimit)

        return ReadingsListResponse(
            readings = page.map { it.toSummary() },
            hasMore = hasMore,
        )
    }

    private fun Reading.toSummary() = ReadingSummary(
        readingId = id.toString(),
        spreadType = spreadType.value,
        question = question,
        cards = cards,
        createdAt = createdAt.toString(),
    )

    private fun Reading.toDetail() = ReadingDetail(
        readingId = id.toString(),
        spreadType = spreadType.value,
        question = question,
        cards = cards,
        interpretation = interpretation ?: "",
        createdAt = createdAt.toString(),
    )
}
