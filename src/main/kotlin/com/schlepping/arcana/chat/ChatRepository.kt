package com.schlepping.arcana.chat

import java.util.UUID

interface ChatRepository {
    suspend fun saveMessages(messages: List<ChatMessage>)
    suspend fun findMessagesByReading(readingId: UUID): List<ChatMessage>
    suspend fun countUserMessagesByReading(readingId: UUID): Int
}
