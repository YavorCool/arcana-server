package com.schlepping.arcana.daily

import com.schlepping.arcana.FakeDailyCardRepository
import com.schlepping.arcana.FakeLlmProvider
import com.schlepping.arcana.llm.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import java.time.DayOfWeek
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DailyCardServiceTest {

    private val fakeLlm = FakeLlmProvider()
    private val fakeRepo = FakeDailyCardRepository()
    private val routingConfig = LlmRoutingConfig(
        premiumReading = "gpt-5",
        freeReading = "gpt-5-mini",
        premiumChat = "gpt-5-mini",
        freeChat = "gpt-5-nano",
        dailyCard = "gpt-5-mini",
        firstReading = "gpt-5",
    )
    private val router = LlmRouter(routingConfig)
    private val promptBuilder = PromptBuilder()
    private val service = DailyCardService(fakeLlm, fakeRepo, router, promptBuilder)
    private val deviceId = UUID.randomUUID()

    @Test
    fun `first request of the day calls LLM and returns cached=false`() = runTest {
        val result = service.getDailyCard(
            deviceId = deviceId,
            cardName = "The Fool",
            isReversed = false,
            tier = UserTier.FREE,
            querentName = null,
        )

        assertFalse(result.cached)
        assertEquals(1, fakeLlm.callCount)
        assertEquals("The Fool", result.cardName)
        assertFalse(result.isReversed)
        assertTrue(result.interpretation.isNotEmpty())
    }

    @Test
    fun `second request same day returns cache and does not call LLM`() = runTest {
        // First call
        service.getDailyCard(deviceId, "The Fool", false, UserTier.FREE, null)
        assertEquals(1, fakeLlm.callCount)

        // Second call
        val result = service.getDailyCard(deviceId, "The Fool", false, UserTier.FREE, null)
        assertTrue(result.cached)
        assertEquals(1, fakeLlm.callCount) // NOT called again
    }

    @Test
    fun `free tier normal day uses brief format`() = runTest {
        service.getDailyCard(deviceId, "The Star", false, UserTier.FREE, null)

        val prompt = fakeLlm.lastPrompt!!
        assertTrue(prompt.userMessage.contains("brief", ignoreCase = true))
    }

    @Test
    fun `premium tier uses full format`() = runTest {
        service.getDailyCard(deviceId, "The Star", false, UserTier.PREMIUM, null)

        val prompt = fakeLlm.lastPrompt!!
        assertTrue(prompt.userMessage.contains("full", ignoreCase = true))
    }

    @Test
    fun `monday free tier uses full format`() = runTest {
        val mondayService = DailyCardService(fakeLlm, fakeRepo, router, promptBuilder) {
            DayOfWeek.MONDAY
        }

        mondayService.getDailyCard(deviceId, "The Moon", true, UserTier.FREE, null)

        val prompt = fakeLlm.lastPrompt!!
        assertTrue(prompt.userMessage.contains("full", ignoreCase = true))
    }

    @Test
    fun `non-monday free tier uses brief format`() = runTest {
        val tuesdayService = DailyCardService(fakeLlm, fakeRepo, router, promptBuilder) {
            DayOfWeek.TUESDAY
        }

        tuesdayService.getDailyCard(deviceId, "The Sun", false, UserTier.FREE, null)

        val prompt = fakeLlm.lastPrompt!!
        assertTrue(prompt.userMessage.contains("brief", ignoreCase = true))
    }

    @Test
    fun `concurrent requests for same device call LLM only once`() = runTest {
        val slowLlm = FakeLlmProvider()
        val slowRepo = FakeDailyCardRepository()
        val slowService = DailyCardService(slowLlm, slowRepo, router, promptBuilder)
        val sharedDeviceId = UUID.randomUUID()

        val results = (1..2).map {
            async {
                slowService.getDailyCard(sharedDeviceId, "The Tower", false, UserTier.FREE, null)
            }
        }.awaitAll()

        assertEquals(1, slowLlm.callCount, "LLM should be called exactly once for concurrent requests")
        val cached = results.count { it.cached }
        val fresh = results.count { !it.cached }
        assertEquals(1, fresh, "Exactly one result should be fresh")
        assertEquals(1, cached, "Exactly one result should be cached")
    }

    @Test
    fun `LLM failure propagates and does not cache`() = runTest {
        val failingLlm = object : LlmProvider {
            var callCount = 0
                private set

            override suspend fun generate(prompt: LlmPrompt): LlmResponse {
                callCount++
                throw LlmException("Service unavailable")
            }
        }
        val failRepo = FakeDailyCardRepository()
        val failService = DailyCardService(failingLlm, failRepo, router, promptBuilder)
        val failDeviceId = UUID.randomUUID()

        assertFailsWith<LlmException> {
            failService.getDailyCard(failDeviceId, "Death", false, UserTier.FREE, null)
        }

        // Nothing should be cached
        val cached = failRepo.findTodaysDailyCard(failDeviceId)
        assertEquals(null, cached, "Failed LLM call should not cache anything")
    }

    private fun runTest(block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) =
        kotlinx.coroutines.test.runTest { block() }
}
