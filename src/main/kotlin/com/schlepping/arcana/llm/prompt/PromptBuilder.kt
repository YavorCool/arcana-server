package com.schlepping.arcana.llm.prompt

import com.schlepping.arcana.llm.LlmPrompt
import com.schlepping.arcana.spread.CardData
import com.schlepping.arcana.spread.SpreadType

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
            DailyCardFormat.BRIEF -> "Provide a brief daily card interpretation (2-3 sentences, capturing the essence)."
            DailyCardFormat.FULL -> "Provide a full daily card interpretation (3-5 sentences plus an invitation to reflect)."
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

    fun buildSpreadPrompt(
        spreadType: SpreadType,
        cards: List<CardData>,
        question: String?,
        querentName: String?,
    ): LlmPrompt {
        val positions = resolvePositions(spreadType)

        val cardsSection = cards.mapIndexed { index, card ->
            val orientation = if (card.isReversed) "reversed" else "upright"
            val position = positions.getOrNull(index)
            if (position != null) {
                "Position: $position — ${card.cardName} ($orientation)"
            } else {
                "${card.cardName} ($orientation)"
            }
        }.joinToString("\n")

        val spreadLabel = spreadType.value.replace("_", " ")
            .replaceFirstChar { it.uppercase() }

        val userMessage = buildString {
            append("Spread type: $spreadLabel\n")
            question?.let { append("Question: $it\n") }
            querentName?.let { append("The querent's name is $it.\n") }
            append("Cards:\n")
            append(cardsSection)
        }

        return LlmPrompt(
            systemMessage = SystemPrompts.READING_V2,
            userMessage = userMessage,
            modelId = "", // filled by caller via LlmRouter
        )
    }

    private fun resolvePositions(spreadType: SpreadType): List<String> = when (spreadType) {
        SpreadType.PAST_PRESENT_FUTURE -> listOf("Past", "Present", "Future")
        SpreadType.YES_NO,
        SpreadType.QUESTION_CARD,
        SpreadType.DAILY_CARD,
        -> emptyList()
    }
}
