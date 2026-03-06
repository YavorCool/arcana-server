package com.schlepping.arcana.spread

import kotlin.test.Test
import kotlin.test.assertFailsWith

class SpreadValidatorTest {

    @Test
    fun `valid YES_NO request passes`() {
        SpreadValidator.validate(
            CreateReadingRequest(
                spreadType = "yes_no",
                cards = listOf(CardData("The Fool", false)),
            ),
        )
    }

    @Test
    fun `valid PAST_PRESENT_FUTURE request passes`() {
        SpreadValidator.validate(
            CreateReadingRequest(
                spreadType = "past_present_future",
                cards = listOf(
                    CardData("The Fool", false),
                    CardData("The Magician", true),
                    CardData("The High Priestess", false),
                ),
            ),
        )
    }

    @Test
    fun `valid QUESTION_CARD request passes`() {
        SpreadValidator.validate(
            CreateReadingRequest(
                spreadType = "question_card",
                question = "What should I focus on?",
                cards = listOf(CardData("Death", false)),
            ),
        )
    }

    @Test
    fun `unknown spreadType throws`() {
        assertFailsWith<IllegalArgumentException>("Unknown spread type") {
            SpreadValidator.validate(
                CreateReadingRequest(
                    spreadType = "unknown_type",
                    cards = listOf(CardData("The Fool", false)),
                ),
            )
        }
    }

    @Test
    fun `wrong card count for YES_NO throws`() {
        assertFailsWith<IllegalArgumentException> {
            SpreadValidator.validate(
                CreateReadingRequest(
                    spreadType = "yes_no",
                    cards = listOf(
                        CardData("The Fool", false),
                        CardData("The Magician", true),
                    ),
                ),
            )
        }
    }

    @Test
    fun `wrong card count for PAST_PRESENT_FUTURE throws`() {
        assertFailsWith<IllegalArgumentException> {
            SpreadValidator.validate(
                CreateReadingRequest(
                    spreadType = "past_present_future",
                    cards = listOf(CardData("The Fool", false)),
                ),
            )
        }
    }

    @Test
    fun `unknown card name throws`() {
        assertFailsWith<IllegalArgumentException>("Unknown card") {
            SpreadValidator.validate(
                CreateReadingRequest(
                    spreadType = "yes_no",
                    cards = listOf(CardData("The Joker", false)),
                ),
            )
        }
    }

    @Test
    fun `duplicate cards throws`() {
        assertFailsWith<IllegalArgumentException>("Duplicate") {
            SpreadValidator.validate(
                CreateReadingRequest(
                    spreadType = "past_present_future",
                    cards = listOf(
                        CardData("The Fool", false),
                        CardData("The Fool", true),
                        CardData("The Magician", false),
                    ),
                ),
            )
        }
    }

    @Test
    fun `question exceeding 500 chars throws`() {
        assertFailsWith<IllegalArgumentException>("500") {
            SpreadValidator.validate(
                CreateReadingRequest(
                    spreadType = "yes_no",
                    question = "x".repeat(501),
                    cards = listOf(CardData("The Fool", false)),
                ),
            )
        }
    }

    @Test
    fun `null question is valid`() {
        SpreadValidator.validate(
            CreateReadingRequest(
                spreadType = "yes_no",
                question = null,
                cards = listOf(CardData("The Fool", false)),
            ),
        )
    }

    @Test
    fun `question exactly 500 chars is valid`() {
        SpreadValidator.validate(
            CreateReadingRequest(
                spreadType = "yes_no",
                question = "x".repeat(500),
                cards = listOf(CardData("The Fool", false)),
            ),
        )
    }

    @Test
    fun `daily_card spread type throws`() {
        assertFailsWith<IllegalArgumentException>("daily card endpoint") {
            SpreadValidator.validate(
                CreateReadingRequest(
                    spreadType = "daily_card",
                    cards = listOf(CardData("The Fool", false)),
                ),
            )
        }
    }

    @Test
    fun `querentName exceeding 100 chars throws`() {
        assertFailsWith<IllegalArgumentException>("100") {
            SpreadValidator.validate(
                CreateReadingRequest(
                    spreadType = "yes_no",
                    cards = listOf(CardData("The Fool", false)),
                    querentName = "x".repeat(101),
                ),
            )
        }
    }

    @Test
    fun `querentName exactly 100 chars is valid`() {
        SpreadValidator.validate(
            CreateReadingRequest(
                spreadType = "yes_no",
                cards = listOf(CardData("The Fool", false)),
                querentName = "x".repeat(100),
            ),
        )
    }
}
