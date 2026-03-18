package com.schlepping.arcana

import com.schlepping.arcana.chat.ChatMessage
import com.schlepping.arcana.chat.ChatRepository
import com.schlepping.arcana.llm.ChatRole
import java.util.UUID

class FakeChatRepository : ChatRepository {

    private val messages = mutableListOf<ChatMessage>()

    override suspend fun saveMessages(messages: List<ChatMessage>) {
        this.messages.addAll(messages)
    }

    override suspend fun findMessagesByReading(readingId: UUID): List<ChatMessage> =
        messages.filter { it.readingId == readingId }.sortedBy { it.createdAt }

    override suspend fun countUserMessagesByReading(readingId: UUID): Int =
        messages.count { it.readingId == readingId && it.role == ChatRole.USER }

    // Test helpers
    fun messageCount(): Int = messages.size
    fun allMessages(): List<ChatMessage> = messages.toList()

    fun clear() {
        messages.clear()
    }
}
