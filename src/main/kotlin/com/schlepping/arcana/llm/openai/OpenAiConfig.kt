package com.schlepping.arcana.llm.openai

data class OpenAiConfig(
    val apiKey: String,
    val baseUrl: String,
    val timeoutMs: Long,
    val maxRetries: Int,
)
