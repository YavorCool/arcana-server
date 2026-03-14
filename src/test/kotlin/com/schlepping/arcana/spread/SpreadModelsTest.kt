package com.schlepping.arcana.spread

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SpreadModelsTest {

    @Test
    fun `fromString parses valid spread types`() {
        assertEquals(SpreadType.YES_NO, SpreadType.fromString("yes_no"))
        assertEquals(SpreadType.PAST_PRESENT_FUTURE, SpreadType.fromString("past_present_future"))
        assertEquals(SpreadType.QUESTION_CARD, SpreadType.fromString("question_card"))
        assertEquals(SpreadType.DAILY_CARD, SpreadType.fromString("daily_card"))
    }

    @Test
    fun `fromString is case-insensitive`() {
        assertEquals(SpreadType.YES_NO, SpreadType.fromString("YES_NO"))
        assertEquals(SpreadType.PAST_PRESENT_FUTURE, SpreadType.fromString("Past_Present_Future"))
    }

    @Test
    fun `fromString returns null for unknown type`() {
        assertNull(SpreadType.fromString("unknown"))
        assertNull(SpreadType.fromString(""))
    }

    @Test
    fun `requiredCardCount matches expected values`() {
        assertEquals(1, SpreadType.YES_NO.requiredCardCount)
        assertEquals(3, SpreadType.PAST_PRESENT_FUTURE.requiredCardCount)
        assertEquals(1, SpreadType.QUESTION_CARD.requiredCardCount)
        assertEquals(1, SpreadType.DAILY_CARD.requiredCardCount)
    }
}
