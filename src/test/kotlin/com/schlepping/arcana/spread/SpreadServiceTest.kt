package com.schlepping.arcana.spread

import com.schlepping.arcana.FakeLlmProvider
import com.schlepping.arcana.FakeSpreadRepository
import com.schlepping.arcana.llm.LlmException
import com.schlepping.arcana.llm.LlmPrompt
import com.schlepping.arcana.llm.LlmProvider
import com.schlepping.arcana.llm.LlmResponse
import com.schlepping.arcana.llm.prompt.PromptBuilder
import com.schlepping.arcana.llm.routing.LlmRouter
import com.schlepping.arcana.llm.routing.LlmRoutingConfig
import com.schlepping.arcana.user.UserTier
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SpreadServiceTest {

    private val fakeLlm = FakeLlmProvider()
    private val fakeRepo = FakeSpreadRepository()
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
    private val service = SpreadService(fakeLlm, fakeRepo, router, promptBuilder)
    private val deviceId = UUID.randomUUID()

    private fun yesNoRequest(cardName: String = "The Fool", question: String? = null) =
        CreateReadingRequest(
            spreadType = "yes_no",
            question = question,
            cards = listOf(CardData(cardName, false)),
        )

    private fun ppfRequest() = CreateReadingRequest(
        spreadType = "past_present_future",
        cards = listOf(
            CardData("The Fool", false),
            CardData("The Magician", true),
            CardData("The High Priestess", false),
        ),
    )

    @Test
    fun `createReading saves and returns interpretation`() = runTest {
        val result = service.createReading(deviceId, yesNoRequest(), UserTier.FREE)

        assertTrue(result.interpretation.isNotEmpty())
        assertTrue(result.readingId.isNotEmpty())
        assertEquals(1, fakeRepo.readingCount())
    }

    @Test
    fun `createReading with invalid card throws IllegalArgumentException`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            service.createReading(
                deviceId,
                CreateReadingRequest(
                    spreadType = "yes_no",
                    cards = listOf(CardData("The Joker", false)),
                ),
                UserTier.FREE,
            )
        }
    }

    @Test
    fun `createReading with wrong card count throws IllegalArgumentException`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            service.createReading(
                deviceId,
                CreateReadingRequest(
                    spreadType = "yes_no",
                    cards = listOf(CardData("The Fool", false), CardData("Death", true)),
                ),
                UserTier.FREE,
            )
        }
    }

    @Test
    fun `free tier first spread ever uses GPT-5`() = runTest {
        service.createReading(deviceId, yesNoRequest(), UserTier.FREE)

        assertEquals("gpt-5", fakeLlm.lastPrompt?.modelId)
    }

    @Test
    fun `free tier second-ever spread uses GPT-5-mini`() = runTest {
        fakeRepo.markCompleted(deviceId)

        service.createReading(deviceId, yesNoRequest(), UserTier.FREE)

        assertEquals("gpt-5-mini", fakeLlm.lastPrompt?.modelId)
    }

    @Test
    fun `premium tier uses GPT-5`() = runTest {
        fakeRepo.markCompleted(deviceId)

        service.createReading(deviceId, yesNoRequest(), UserTier.PREMIUM)

        assertEquals("gpt-5", fakeLlm.lastPrompt?.modelId)
    }

    @Test
    fun `free tier second spread of the day throws DailyLimitReached`() = runTest {
        fakeRepo.setTodaySpreads(deviceId, 1)

        assertFailsWith<SpreadException.DailyLimitReached> {
            service.createReading(deviceId, yesNoRequest(), UserTier.FREE)
        }
    }

    @Test
    fun `premium tier has no daily limit`() = runTest {
        fakeRepo.setTodaySpreads(deviceId, 100)
        fakeRepo.markCompleted(deviceId)

        val result = service.createReading(deviceId, yesNoRequest(), UserTier.PREMIUM)

        assertTrue(result.interpretation.isNotEmpty())
    }

    @Test
    fun `first spread marks hasCompletedSpread`() = runTest {
        service.createReading(deviceId, yesNoRequest(), UserTier.FREE)

        assertTrue(fakeRepo.hasCompletedSpread(deviceId))
    }

    @Test
    fun `LLM failure does not save reading`() = runTest {
        val failingLlm = object : LlmProvider {
            override suspend fun generate(prompt: LlmPrompt): LlmResponse {
                throw LlmException("Service unavailable")
            }
        }
        val failService = SpreadService(failingLlm, fakeRepo, router, promptBuilder)

        assertFailsWith<LlmException> {
            failService.createReading(deviceId, yesNoRequest(), UserTier.FREE)
        }

        assertEquals(0, fakeRepo.readingCount())
        assertEquals(0, fakeRepo.todaySpreads(deviceId))
    }

    @Test
    fun `createReading passes question to prompt`() = runTest {
        service.createReading(deviceId, yesNoRequest(question = "Will I find love?"), UserTier.FREE)

        assertTrue(fakeLlm.lastPrompt!!.userMessage.contains("Will I find love?"))
    }

    @Test
    fun `createReading PPF includes position labels in prompt`() = runTest {
        service.createReading(deviceId, ppfRequest(), UserTier.FREE)

        val userMessage = fakeLlm.lastPrompt!!.userMessage
        assertTrue(userMessage.contains("Past"))
        assertTrue(userMessage.contains("Present"))
        assertTrue(userMessage.contains("Future"))
    }

    // getReading tests

    @Test
    fun `getReading returns reading detail`() = runTest {
        val result = service.createReading(deviceId, yesNoRequest(), UserTier.FREE)

        val detail = service.getReading(deviceId, UUID.fromString(result.readingId))

        assertEquals(result.readingId, detail.readingId)
        assertEquals(result.interpretation, detail.interpretation)
        assertEquals("yes_no", detail.spreadType)
    }

    @Test
    fun `getReading for non-existent reading throws`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            service.getReading(deviceId, UUID.randomUUID())
        }
    }

    @Test
    fun `getReading for different device throws`() = runTest {
        val result = service.createReading(deviceId, yesNoRequest(), UserTier.FREE)
        val otherDevice = UUID.randomUUID()

        assertFailsWith<IllegalArgumentException> {
            service.getReading(otherDevice, UUID.fromString(result.readingId))
        }
    }

    // listReadings tests

    @Test
    fun `listReadings returns readings in reverse chronological order`() = runTest {
        service.createReading(deviceId, yesNoRequest("The Fool"), UserTier.FREE)
        fakeRepo.setTodaySpreads(deviceId, 0) // reset for next
        service.createReading(deviceId, yesNoRequest("Death"), UserTier.FREE)

        val list = service.listReadings(deviceId)

        assertEquals(2, list.readings.size)
        // Most recent first
        assertTrue(list.readings[0].cards[0].cardName == "Death")
        assertTrue(list.readings[1].cards[0].cardName == "The Fool")
    }

    @Test
    fun `listReadings respects limit`() = runTest {
        repeat(3) {
            fakeRepo.setTodaySpreads(deviceId, 0)
            service.createReading(deviceId, yesNoRequest(), UserTier.FREE)
        }

        val list = service.listReadings(deviceId, limit = 2)

        assertEquals(2, list.readings.size)
        assertTrue(list.hasMore)
    }

    @Test
    fun `listReadings returns hasMore=false for last page`() = runTest {
        service.createReading(deviceId, yesNoRequest(), UserTier.FREE)

        val list = service.listReadings(deviceId)

        assertEquals(1, list.readings.size)
        assertEquals(false, list.hasMore)
    }

    @Test
    fun `listReadings clamps limit to 50 max`() = runTest {
        val list = service.listReadings(deviceId, limit = 100)

        assertEquals(false, list.hasMore) // empty but no crash
    }

    @Test
    fun `listReadings returns empty list for device with no readings`() = runTest {
        val list = service.listReadings(UUID.randomUUID())

        assertEquals(0, list.readings.size)
        assertEquals(false, list.hasMore)
    }

    private fun runTest(block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) =
        kotlinx.coroutines.test.runTest { block() }
}
