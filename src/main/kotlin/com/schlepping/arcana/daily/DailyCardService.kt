package com.schlepping.arcana.daily

import com.schlepping.arcana.llm.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class DailyCardService(
    private val llmProvider: LlmProvider,
    private val repository: DailyCardRepository,
    private val router: LlmRouter,
    private val promptBuilder: PromptBuilder,
    private val dayOfWeekProvider: () -> DayOfWeek = { LocalDate.now(ZoneOffset.UTC).dayOfWeek },
) {

    private val locks = ConcurrentHashMap<UUID, Mutex>()

    suspend fun getDailyCard(
        deviceId: UUID,
        cardName: String,
        isReversed: Boolean,
        tier: UserTier,
        querentName: String?,
    ): DailyCardResponse {
        val mutex = locks.computeIfAbsent(deviceId) { Mutex() }
        return mutex.withLock {
            // Check cache
            repository.findTodaysDailyCard(deviceId)?.let { cached ->
                return@withLock DailyCardResponse(
                    readingId = cached.id.toString(),
                    cardName = cached.cardName,
                    isReversed = cached.isReversed,
                    interpretation = cached.interpretation,
                    cached = true,
                )
            }

            // Determine format
            val format = resolveFormat(tier)

            // Build prompt + resolve model
            val modelId = router.resolve(tier, RequestType.DAILY_CARD)
            val prompt = promptBuilder.buildDailyCardPrompt(cardName, isReversed, format, querentName)
                .copy(modelId = modelId)

            // Call LLM
            val llmResponse = llmProvider.generate(prompt)

            // Save
            val readingId = UUID.randomUUID()
            repository.saveDailyCard(readingId, deviceId, cardName, isReversed, llmResponse.content)

            DailyCardResponse(
                readingId = readingId.toString(),
                cardName = cardName,
                isReversed = isReversed,
                interpretation = llmResponse.content,
                cached = false,
            )
        }
    }

    private fun resolveFormat(tier: UserTier): DailyCardFormat = when {
        tier == UserTier.PREMIUM -> DailyCardFormat.FULL
        dayOfWeekProvider() == DayOfWeek.MONDAY -> DailyCardFormat.FULL
        else -> DailyCardFormat.BRIEF
    }
}
