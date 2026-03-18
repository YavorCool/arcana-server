package com.schlepping.arcana.chat

import com.schlepping.arcana.llm.ChatRole
import com.schlepping.arcana.llm.LlmProvider
import com.schlepping.arcana.llm.RequestType
import com.schlepping.arcana.llm.prompt.PromptBuilder
import com.schlepping.arcana.llm.routing.LlmRouter
import com.schlepping.arcana.spread.SpreadRepository
import com.schlepping.arcana.user.UserTier
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

private const val MAX_MESSAGE_LENGTH = 1000
private const val HISTORY_WINDOW_SIZE = 20
private const val FREE_CHAT_LIMIT = 1
private const val PREMIUM_CHAT_LIMIT = 50

class ChatService(
    private val chatRepository: ChatRepository,
    private val spreadRepository: SpreadRepository,
    private val llmProvider: LlmProvider,
    private val router: LlmRouter,
    private val promptBuilder: PromptBuilder,
) {

    suspend fun sendMessage(
        deviceId: UUID,
        readingId: UUID,
        message: String,
        tier: UserTier,
    ): ChatMessageResponse {
        require(message.isNotBlank()) { "Message must not be blank" }
        require(message.length <= MAX_MESSAGE_LENGTH) { "Message must not exceed $MAX_MESSAGE_LENGTH characters" }

        val reading = spreadRepository.findReadingById(readingId, deviceId)
            ?: throw ChatException.ReadingNotFound()

        val maxMessages = when (tier) {
            UserTier.FREE -> FREE_CHAT_LIMIT
            UserTier.PREMIUM -> PREMIUM_CHAT_LIMIT
        }
        if (chatRepository.countUserMessagesByReading(readingId) >= maxMessages) {
            throw ChatException.ChatLimitReached()
        }

        val chatHistory = chatRepository.findMessagesByReading(readingId)
        val windowedHistory = chatHistory.takeLast(HISTORY_WINDOW_SIZE)

        val modelId = router.resolve(tier, RequestType.CHAT)
        val prompt = promptBuilder.buildChatPrompt(
            reading = reading,
            chatHistory = windowedHistory,
            userMessage = message,
            querentName = null,
        ).copy(modelId = modelId)

        val llmResponse = llmProvider.generate(prompt)

        val now = LocalDateTime.now(ZoneOffset.UTC)
        val userMsg = ChatMessage(
            id = UUID.randomUUID(),
            readingId = readingId,
            role = ChatRole.USER,
            text = message,
            createdAt = now,
        )
        val assistantMsg = ChatMessage(
            id = UUID.randomUUID(),
            readingId = readingId,
            role = ChatRole.ASSISTANT,
            text = llmResponse.content,
            createdAt = now,
        )
        chatRepository.saveMessages(listOf(userMsg, assistantMsg))

        return ChatMessageResponse(aiResponse = llmResponse.content)
    }

    suspend fun getHistory(readingId: UUID): List<ChatMessage> =
        chatRepository.findMessagesByReading(readingId)
}
