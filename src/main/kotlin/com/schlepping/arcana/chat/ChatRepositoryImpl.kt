package com.schlepping.arcana.chat

import com.schlepping.arcana.db.ChatMessages
import com.schlepping.arcana.llm.ChatRole
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class ChatRepositoryImpl : ChatRepository {

    override suspend fun saveMessages(messages: List<ChatMessage>): Unit = newSuspendedTransaction {
        messages.forEach { msg ->
            ChatMessages.insert {
                it[id] = msg.id
                it[readingId] = msg.readingId
                it[role] = msg.role.apiValue
                it[text] = msg.text
                it[createdAt] = msg.createdAt
            }
        }
    }

    override suspend fun findMessagesByReading(readingId: UUID): List<ChatMessage> =
        newSuspendedTransaction {
            ChatMessages.selectAll()
                .where { ChatMessages.readingId eq readingId }
                .orderBy(ChatMessages.createdAt, SortOrder.ASC)
                .map { row ->
                    ChatMessage(
                        id = row[ChatMessages.id],
                        readingId = row[ChatMessages.readingId],
                        role = ChatRole.entries.first { it.apiValue == row[ChatMessages.role] },
                        text = row[ChatMessages.text],
                        createdAt = row[ChatMessages.createdAt],
                    )
                }
        }

    override suspend fun countUserMessagesByReading(readingId: UUID): Int =
        newSuspendedTransaction {
            ChatMessages.selectAll()
                .where {
                    (ChatMessages.readingId eq readingId) and
                        (ChatMessages.role eq ChatRole.USER.apiValue)
                }
                .count()
                .toInt()
        }
}
