package com.schlepping.arcana.llm

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
