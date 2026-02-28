package com.schlepping.arcana.llm

enum class DailyCardFormat { BRIEF, FULL }

class PromptBuilder {

    fun buildDailyCardPrompt(
        cardName: String,
        isReversed: Boolean,
        format: DailyCardFormat,
        querentName: String?,
    ): LlmPrompt {
        val orientation = if (isReversed) "reversed" else "upright"

        val formatInstruction = when (format) {
            DailyCardFormat.BRIEF -> "Provide a brief daily card interpretation (2-3 short paragraphs)."
            DailyCardFormat.FULL -> "Provide a full, detailed daily card interpretation (4-5 paragraphs with deeper insight)."
        }

        val querentSection = querentName?.let { "The querent's name is $it. " } ?: ""

        val userMessage = buildString {
            append("Daily card drawn: $cardName ($orientation). ")
            append(querentSection)
            append(formatInstruction)
        }

        return LlmPrompt(
            systemMessage = SystemPrompts.READING_V2,
            userMessage = userMessage,
            modelId = "", // filled by caller via LlmRouter
        )
    }
}
