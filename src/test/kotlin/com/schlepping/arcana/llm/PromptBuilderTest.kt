package com.schlepping.arcana.llm

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

        assertTrue(prompt.systemMessage.contains("Arcana"))
        assertTrue(prompt.systemMessage.contains("tarot"))
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
}
