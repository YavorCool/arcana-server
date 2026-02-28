package com.schlepping.arcana.llm

data class OpenAiConfig(
    val apiKey: String,
    val baseUrl: String,
    val timeoutMs: Long,
    val maxRetries: Int,
)

data class LlmRoutingConfig(
    val premiumReading: String,
    val freeReading: String,
    val premiumChat: String,
    val freeChat: String,
    val dailyCard: String,
    val firstReading: String,
)
