package com.schlepping.arcana.llm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class UserTier { FREE, PREMIUM }

enum class RequestType { READING, CHAT, DAILY_CARD }

data class LlmPrompt(
    val systemMessage: String,
    val userMessage: String,
    val modelId: String,
)

data class LlmResponse(
    val content: String,
    val modelId: String,
    val promptTokens: Int,
    val completionTokens: Int,
)

class LlmException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

// OpenAI API DTOs

@Serializable
data class OpenAiRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Double = 0.7,
    @SerialName("max_tokens") val maxTokens: Int = 2048,
)

@Serializable
data class OpenAiMessage(
    val role: String,
    val content: String,
)

@Serializable
data class OpenAiResponse(
    val id: String,
    val choices: List<OpenAiChoice>,
    val usage: OpenAiUsage,
    val model: String,
)

@Serializable
data class OpenAiChoice(
    val index: Int,
    val message: OpenAiMessage,
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
data class OpenAiUsage(
    @SerialName("prompt_tokens") val promptTokens: Int,
    @SerialName("completion_tokens") val completionTokens: Int,
    @SerialName("total_tokens") val totalTokens: Int,
)
