package com.schlepping.arcana.llm

interface LlmProvider {
    suspend fun generate(prompt: LlmPrompt): LlmResponse
}
