package com.schlepping.arcana.llm.routing

/**
 * OpenAI model IDs for each tier/request-type combination.
 * Values come from application.yaml and are passed as-is
 * in the "model" field of OpenAI API requests.
 * Example: "gpt-5", "gpt-5-mini", "gpt-5-nano"
 */
data class LlmRoutingConfig(
    val premiumReading: String,
    val freeReading: String,
    val premiumChat: String,
    val freeChat: String,
    val dailyCard: String,
    val firstReading: String,
)
