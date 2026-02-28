package com.schlepping.arcana.daily

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class DailyCardRepositoryTest {

    @Test
    fun `JSON round-trip with normal card name`() {
        val original = CardData(cardName = "The Fool", isReversed = false)
        val json = Json.encodeToString(original)
        val decoded = Json.decodeFromString<CardData>(json)
        assertEquals(original, decoded)
    }

    @Test
    fun `JSON round-trip with apostrophe in card name`() {
        val original = CardData(cardName = "The Fool's Journey", isReversed = true)
        val json = Json.encodeToString(original)
        val decoded = Json.decodeFromString<CardData>(json)
        assertEquals(original, decoded)
    }

    @Test
    fun `JSON round-trip with quotes in card name`() {
        val original = CardData(cardName = """Card "X"""", isReversed = false)
        val json = Json.encodeToString(original)
        val decoded = Json.decodeFromString<CardData>(json)
        assertEquals(original, decoded)
    }

    @Test
    fun `JSON round-trip preserves isReversed true`() {
        val original = CardData(cardName = "The Tower", isReversed = true)
        val json = Json.encodeToString(original)
        val decoded = Json.decodeFromString<CardData>(json)
        assertEquals(true, decoded.isReversed)
    }

    @Test
    fun `JSON round-trip preserves isReversed false`() {
        val original = CardData(cardName = "The Star", isReversed = false)
        val json = Json.encodeToString(original)
        val decoded = Json.decodeFromString<CardData>(json)
        assertEquals(false, decoded.isReversed)
    }

    @Test
    fun `JSON round-trip with special characters in card name`() {
        val original = CardData(cardName = "Ace of Cups — reversed\n\t", isReversed = true)
        val json = Json.encodeToString(original)
        val decoded = Json.decodeFromString<CardData>(json)
        assertEquals(original, decoded)
    }
}
