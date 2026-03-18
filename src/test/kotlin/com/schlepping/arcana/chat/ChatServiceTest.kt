package com.schlepping.arcana.chat

import com.schlepping.arcana.FakeChatRepository
import com.schlepping.arcana.FakeLlmProvider
import com.schlepping.arcana.FakeSpreadRepository
import com.schlepping.arcana.llm.ChatRole
import com.schlepping.arcana.llm.LlmException
import com.schlepping.arcana.llm.LlmPrompt
import com.schlepping.arcana.llm.LlmProvider
import com.schlepping.arcana.llm.LlmResponse
import com.schlepping.arcana.llm.prompt.PromptBuilder
import com.schlepping.arcana.llm.prompt.SystemPrompts
import com.schlepping.arcana.llm.routing.LlmRouter
import com.schlepping.arcana.llm.routing.LlmRoutingConfig
import com.schlepping.arcana.spread.CardData
import com.schlepping.arcana.spread.Reading
import com.schlepping.arcana.spread.SpreadType
import com.schlepping.arcana.user.UserTier
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ChatServiceTest {

    private val fakeLlm = FakeLlmProvider()
    private val fakeChatRepo = FakeChatRepository()
    private val fakeSpreadRepo = FakeSpreadRepository()
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
    private val service = ChatService(fakeChatRepo, fakeSpreadRepo, fakeLlm, router, promptBuilder)
    private val deviceId = UUID.randomUUID()

    private fun createReading(
        device: UUID = deviceId,
        question: String? = "Will I find love?",
    ): Reading {
        val reading = Reading(
            id = UUID.randomUUID(),
            deviceId = device,
            spreadType = SpreadType.YES_NO,
            question = question,
            cards = listOf(CardData("The Fool", false)),
            interpretation = "The Fool speaks of new beginnings...",
            createdAt = LocalDateTime.now(ZoneOffset.UTC),
        )
        return reading
    }

    private suspend fun seedReading(reading: Reading) {
        fakeSpreadRepo.saveReading(reading)
    }

    @Test
    fun `sendMessage saves and returns AI response`() = runTest {
        val reading = createReading()
        seedReading(reading)
        fakeLlm.response = "The cards suggest reflection."

        val result = service.sendMessage(deviceId, reading.id, "What does this mean?", UserTier.FREE)

        assertEquals("The cards suggest reflection.", result.aiResponse)
        assertEquals(2, fakeChatRepo.messageCount())
        val messages = fakeChatRepo.allMessages()
        assertEquals(ChatRole.USER, messages[0].role)
        assertEquals("What does this mean?", messages[0].text)
        assertEquals(ChatRole.ASSISTANT, messages[1].role)
        assertEquals("The cards suggest reflection.", messages[1].text)
    }

    @Test
    fun `sendMessage with blank message throws`() = runTest {
        val reading = createReading()
        seedReading(reading)

        assertFailsWith<IllegalArgumentException> {
            service.sendMessage(deviceId, reading.id, "   ", UserTier.FREE)
        }
    }

    @Test
    fun `sendMessage with message over 1000 chars throws`() = runTest {
        val reading = createReading()
        seedReading(reading)

        assertFailsWith<IllegalArgumentException> {
            service.sendMessage(deviceId, reading.id, "a".repeat(1001), UserTier.FREE)
        }
    }

    @Test
    fun `sendMessage to non-existent reading throws ReadingNotFound`() = runTest {
        assertFailsWith<ChatException.ReadingNotFound> {
            service.sendMessage(deviceId, UUID.randomUUID(), "Hello", UserTier.FREE)
        }
    }

    @Test
    fun `sendMessage to another device's reading throws ReadingNotFound`() = runTest {
        val reading = createReading(device = UUID.randomUUID())
        seedReading(reading)

        assertFailsWith<ChatException.ReadingNotFound> {
            service.sendMessage(deviceId, reading.id, "Hello", UserTier.FREE)
        }
    }

    @Test
    fun `free tier second message to same reading throws ChatLimitReached`() = runTest {
        val reading = createReading()
        seedReading(reading)

        service.sendMessage(deviceId, reading.id, "First question", UserTier.FREE)

        assertFailsWith<ChatException.ChatLimitReached> {
            service.sendMessage(deviceId, reading.id, "Second question", UserTier.FREE)
        }
    }

    @Test
    fun `premium tier allows multiple messages`() = runTest {
        val reading = createReading()
        seedReading(reading)

        service.sendMessage(deviceId, reading.id, "First question", UserTier.PREMIUM)
        service.sendMessage(deviceId, reading.id, "Second question", UserTier.PREMIUM)
        service.sendMessage(deviceId, reading.id, "Third question", UserTier.PREMIUM)

        assertEquals(6, fakeChatRepo.messageCount()) // 3 user + 3 assistant
    }

    @Test
    fun `sendMessage includes chat history in prompt`() = runTest {
        val reading = createReading()
        seedReading(reading)

        service.sendMessage(deviceId, reading.id, "First question", UserTier.PREMIUM)
        service.sendMessage(deviceId, reading.id, "Follow up", UserTier.PREMIUM)

        val lastPrompt = fakeLlm.lastPrompt!!
        assertEquals(2, lastPrompt.conversationHistory.size)
        assertEquals(ChatRole.USER, lastPrompt.conversationHistory[0].role)
        assertEquals("First question", lastPrompt.conversationHistory[0].content)
        assertEquals(ChatRole.ASSISTANT, lastPrompt.conversationHistory[1].role)
    }

    @Test
    fun `sendMessage uses correct model for free tier`() = runTest {
        val reading = createReading()
        seedReading(reading)

        service.sendMessage(deviceId, reading.id, "Question", UserTier.FREE)

        assertEquals("gpt-5-nano", fakeLlm.lastPrompt?.modelId)
    }

    @Test
    fun `sendMessage uses correct model for premium tier`() = runTest {
        val reading = createReading()
        seedReading(reading)

        service.sendMessage(deviceId, reading.id, "Question", UserTier.PREMIUM)

        assertEquals("gpt-5-mini", fakeLlm.lastPrompt?.modelId)
    }

    @Test
    fun `LLM failure does not save messages`() = runTest {
        val reading = createReading()
        seedReading(reading)
        val failingLlm = object : LlmProvider {
            override suspend fun generate(prompt: LlmPrompt): LlmResponse {
                throw LlmException("Service unavailable")
            }
        }
        val failService = ChatService(fakeChatRepo, fakeSpreadRepo, failingLlm, router, promptBuilder)

        assertFailsWith<LlmException> {
            failService.sendMessage(deviceId, reading.id, "Question", UserTier.FREE)
        }

        assertEquals(0, fakeChatRepo.messageCount())
    }

    @Test
    fun `sendMessage uses CHAT_V2 system prompt`() = runTest {
        val reading = createReading()
        seedReading(reading)

        service.sendMessage(deviceId, reading.id, "Question", UserTier.FREE)

        assertTrue(fakeLlm.lastPrompt!!.systemMessage.contains(SystemPrompts.CHAT_V2))
    }

    @Test
    fun `prompt includes reading context (cards, question, interpretation)`() = runTest {
        val reading = createReading(question = "Will I find love?")
        seedReading(reading)

        service.sendMessage(deviceId, reading.id, "Tell me more", UserTier.FREE)

        val systemMsg = fakeLlm.lastPrompt!!.systemMessage
        assertTrue(systemMsg.contains("The Fool"))
        assertTrue(systemMsg.contains("Will I find love?"))
        assertTrue(systemMsg.contains("The Fool speaks of new beginnings..."))
    }

    @Test
    fun `history windowing limits to last 20 messages`() = runTest {
        val reading = createReading()
        seedReading(reading)

        // Pre-populate 24 messages (12 pairs)
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val oldMessages = (1..12).flatMap { i ->
            listOf(
                ChatMessage(
                    id = UUID.randomUUID(),
                    readingId = reading.id,
                    role = ChatRole.USER,
                    text = "Question $i",
                    createdAt = now.plusSeconds(i.toLong() * 2 - 1),
                ),
                ChatMessage(
                    id = UUID.randomUUID(),
                    readingId = reading.id,
                    role = ChatRole.ASSISTANT,
                    text = "Answer $i",
                    createdAt = now.plusSeconds(i.toLong() * 2),
                ),
            )
        }
        fakeChatRepo.saveMessages(oldMessages)

        service.sendMessage(deviceId, reading.id, "New question", UserTier.PREMIUM)

        val lastPrompt = fakeLlm.lastPrompt!!
        // 24 existing messages, windowed to last 20
        assertEquals(20, lastPrompt.conversationHistory.size)
        // First message in window should be Question 3 (skipping Q1/A1 and Q2/A2)
        assertEquals("Question 3", lastPrompt.conversationHistory[0].content)
    }

    private fun runTest(block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) =
        kotlinx.coroutines.test.runTest { block() }
}
