package com.schlepping.arcana.chat

import com.schlepping.arcana.llm.ChatRole
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class ChatMessageRequest(val message: String)

@Serializable
data class ChatMessageResponse(val aiResponse: String)

@Serializable
data class ChatMessageDto(
    val id: String,
    val role: ChatRole,
    val text: String,
    val createdAt: String,
)

data class ChatMessage(
    val id: UUID,
    val readingId: UUID,
    val role: ChatRole,
    val text: String,
    val createdAt: LocalDateTime,
)

sealed class ChatException(message: String) : RuntimeException(message) {
    class ChatLimitReached : ChatException("Chat message limit reached for this reading")
    class ReadingNotFound : ChatException("Reading not found")
}
