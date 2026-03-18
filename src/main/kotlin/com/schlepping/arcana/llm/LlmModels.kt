package com.schlepping.arcana.llm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class RequestType { READING, CHAT, DAILY_CARD }

@Serializable
enum class ChatRole(val apiValue: String) {
    @SerialName("user") USER("user"),
    @SerialName("assistant") ASSISTANT("assistant"),
}

data class ChatTurn(val role: ChatRole, val content: String)

data class LlmPrompt(
    val systemMessage: String,
    val userMessage: String,
    val modelId: String,
    val conversationHistory: List<ChatTurn> = emptyList(),
)

data class LlmResponse(
    val content: String,
    val modelId: String,
    val promptTokens: Int,
    val completionTokens: Int,
)
