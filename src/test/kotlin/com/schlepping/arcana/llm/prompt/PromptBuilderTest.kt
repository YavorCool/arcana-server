package com.schlepping.arcana.llm.prompt

import com.schlepping.arcana.spread.CardData
import com.schlepping.arcana.spread.SpreadType
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class PromptBuilderTest {

    private val builder = PromptBuilder()

    @Test
    fun `daily card brief format contains brief instruction`() {
        val prompt = builder.buildDailyCardPrompt(
            cardName = "The Fool",
            isReversed = false,
            format = DailyCardFormat.BRIEF,
            querentName = null,
        )

        assertTrue(prompt.userMessage.contains("brief", ignoreCase = true))
        assertTrue(prompt.userMessage.contains("The Fool"))
        assertTrue(prompt.systemMessage.isNotEmpty())
    }

    @Test
    fun `daily card full format contains full instruction`() {
        val prompt = builder.buildDailyCardPrompt(
            cardName = "The Tower",
            isReversed = false,
            format = DailyCardFormat.FULL,
            querentName = null,
        )

        assertTrue(prompt.userMessage.contains("full", ignoreCase = true))
        assertFalse(prompt.userMessage.contains("brief", ignoreCase = true))
        assertTrue(prompt.userMessage.contains("The Tower"))
    }

    @Test
    fun `querent name included when provided`() {
        val prompt = builder.buildDailyCardPrompt(
            cardName = "The Star",
            isReversed = false,
            format = DailyCardFormat.BRIEF,
            querentName = "Nikita",
        )

        assertTrue(prompt.userMessage.contains("Nikita"))
    }

    @Test
    fun `querent name omitted when null`() {
        val prompt = builder.buildDailyCardPrompt(
            cardName = "The Star",
            isReversed = false,
            format = DailyCardFormat.BRIEF,
            querentName = null,
        )

        assertFalse(prompt.userMessage.contains("querent", ignoreCase = true))
    }

    @Test
    fun `reversed card noted in prompt`() {
        val prompt = builder.buildDailyCardPrompt(
            cardName = "The Moon",
            isReversed = true,
            format = DailyCardFormat.FULL,
            querentName = null,
        )

        assertTrue(prompt.userMessage.contains("reversed", ignoreCase = true))
        assertTrue(prompt.userMessage.contains("The Moon"))
    }

    @Test
    fun `upright card noted in prompt`() {
        val prompt = builder.buildDailyCardPrompt(
            cardName = "The Sun",
            isReversed = false,
            format = DailyCardFormat.FULL,
            querentName = null,
        )

        assertTrue(prompt.userMessage.contains("upright", ignoreCase = true))
        assertTrue(prompt.userMessage.contains("The Sun"))
    }

    @Test
    fun `uses reading system prompt`() {
        val prompt = builder.buildDailyCardPrompt(
            cardName = "The Fool",
            isReversed = false,
            format = DailyCardFormat.BRIEF,
            querentName = null,
        )

        assertTrue(prompt.systemMessage.contains("ARCANA", ignoreCase = true))
        assertTrue(prompt.systemMessage.contains("tarot", ignoreCase = true))
    }

    @Test
    fun `card name with special characters included correctly in prompt`() {
        val prompt = builder.buildDailyCardPrompt(
            cardName = """The Fool's "Journey"""",
            isReversed = true,
            format = DailyCardFormat.BRIEF,
            querentName = null,
        )

        assertTrue(prompt.userMessage.contains("""The Fool's "Journey""""))
        assertTrue(prompt.userMessage.contains("reversed"))
    }

    // Spread prompt tests

    @Test
    fun `spread prompt for PPF includes position labels`() {
        val prompt = builder.buildSpreadPrompt(
            spreadType = SpreadType.PAST_PRESENT_FUTURE,
            cards = listOf(
                CardData("The Fool", false),
                CardData("The Magician", true),
                CardData("The High Priestess", false),
            ),
            question = null,
            querentName = null,
        )

        assertTrue(prompt.userMessage.contains("Past"))
        assertTrue(prompt.userMessage.contains("Present"))
        assertTrue(prompt.userMessage.contains("Future"))
    }

    @Test
    fun `spread prompt for YES_NO has no position labels`() {
        val prompt = builder.buildSpreadPrompt(
            spreadType = SpreadType.YES_NO,
            cards = listOf(CardData("The Fool", false)),
            question = null,
            querentName = null,
        )

        assertFalse(prompt.userMessage.contains("Position:"))
        assertTrue(prompt.userMessage.contains("The Fool"))
    }

    @Test
    fun `spread prompt includes question when provided`() {
        val prompt = builder.buildSpreadPrompt(
            spreadType = SpreadType.QUESTION_CARD,
            cards = listOf(CardData("The Star", false)),
            question = "Will I find love?",
            querentName = null,
        )

        assertTrue(prompt.userMessage.contains("Will I find love?"))
    }

    @Test
    fun `spread prompt omits question when null`() {
        val prompt = builder.buildSpreadPrompt(
            spreadType = SpreadType.YES_NO,
            cards = listOf(CardData("The Star", false)),
            question = null,
            querentName = null,
        )

        assertFalse(prompt.userMessage.contains("Question:"))
    }

    @Test
    fun `spread prompt includes querent name when provided`() {
        val prompt = builder.buildSpreadPrompt(
            spreadType = SpreadType.YES_NO,
            cards = listOf(CardData("The Star", false)),
            question = null,
            querentName = "Nikita",
        )

        assertTrue(prompt.userMessage.contains("Nikita"))
    }

    @Test
    fun `spread prompt shows reversed for reversed card`() {
        val prompt = builder.buildSpreadPrompt(
            spreadType = SpreadType.YES_NO,
            cards = listOf(CardData("Death", true)),
            question = null,
            querentName = null,
        )

        assertTrue(prompt.userMessage.contains("reversed"))
        assertTrue(prompt.userMessage.contains("Death"))
    }

    @Test
    fun `spread prompt uses READING_V2 system prompt`() {
        val prompt = builder.buildSpreadPrompt(
            spreadType = SpreadType.YES_NO,
            cards = listOf(CardData("The Fool", false)),
            question = null,
            querentName = null,
        )

        assertTrue(prompt.systemMessage.contains("ARCANA"))
    }

    @Test
    fun `spread prompt includes spread type label`() {
        val prompt = builder.buildSpreadPrompt(
            spreadType = SpreadType.PAST_PRESENT_FUTURE,
            cards = listOf(
                CardData("The Fool", false),
                CardData("The Magician", false),
                CardData("The Star", false),
            ),
            question = null,
            querentName = null,
        )

        assertTrue(prompt.userMessage.contains("Past present future"))
    }
}
