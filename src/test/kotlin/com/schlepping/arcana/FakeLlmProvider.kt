package com.schlepping.arcana

import com.schlepping.arcana.llm.LlmPrompt
import com.schlepping.arcana.llm.LlmProvider
import com.schlepping.arcana.llm.LlmResponse

class FakeLlmProvider : LlmProvider {

    var response = "Fake interpretation for your daily card."
    var callCount = 0
        private set
    var lastPrompt: LlmPrompt? = null
        private set

    override suspend fun generate(prompt: LlmPrompt): LlmResponse {
        callCount++
        lastPrompt = prompt
        return LlmResponse(
            content = response,
            modelId = prompt.modelId,
            promptTokens = 100,
            completionTokens = 50,
        )
    }

    fun reset() {
        callCount = 0
        lastPrompt = null
        response = "Fake interpretation for your daily card."
    }
}
