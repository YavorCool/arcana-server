package com.schlepping.arcana.tarot

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TarotDeckTest {

    @Test
    fun `allCards contains exactly 78 cards`() {
        assertEquals(78, TarotDeck.allCards.size)
    }

    @Test
    fun `no duplicate names in deck`() {
        val names = TarotDeck.allCards.map { it.name }
        assertEquals(names.size, names.toSet().size, "Duplicate card names found")
    }

    @Test
    fun `no duplicate ids in deck`() {
        val ids = TarotDeck.allCards.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "Duplicate card ids found")
    }

    @Test
    fun `validCardNames contains 78 entries`() {
        assertEquals(78, TarotDeck.validCardNames.size)
    }

    @Test
    fun `isValidCard returns true for known major arcana`() {
        assertTrue(TarotDeck.isValidCard("The Fool"))
        assertTrue(TarotDeck.isValidCard("The World"))
        assertTrue(TarotDeck.isValidCard("Death"))
    }

    @Test
    fun `isValidCard returns true for known minor arcana`() {
        assertTrue(TarotDeck.isValidCard("Ace of Cups"))
        assertTrue(TarotDeck.isValidCard("King of Swords"))
        assertTrue(TarotDeck.isValidCard("Ten of Wands"))
        assertTrue(TarotDeck.isValidCard("Page of Pentacles"))
    }

    @Test
    fun `isValidCard returns false for unknown card`() {
        assertFalse(TarotDeck.isValidCard("The Joker"))
        assertFalse(TarotDeck.isValidCard(""))
        assertFalse(TarotDeck.isValidCard("Ace of Hearts"))
    }

    @Test
    fun `isValidCard is case-sensitive`() {
        assertFalse(TarotDeck.isValidCard("the fool"))
        assertFalse(TarotDeck.isValidCard("THE FOOL"))
        assertFalse(TarotDeck.isValidCard("ace of cups"))
    }

    @Test
    fun `major arcana has 22 cards`() {
        assertEquals(22, TarotDeck.majorArcana.size)
    }

    @Test
    fun `each minor suit has 14 cards`() {
        assertEquals(14, TarotDeck.cups.size)
        assertEquals(14, TarotDeck.swords.size)
        assertEquals(14, TarotDeck.wands.size)
        assertEquals(14, TarotDeck.pentacles.size)
    }
}
