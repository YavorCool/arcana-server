package com.schlepping.arcana.tarot

import com.schlepping.arcana.tarot.deck.cupsCards
import com.schlepping.arcana.tarot.deck.majorArcanaCards
import com.schlepping.arcana.tarot.deck.pentaclesCards
import com.schlepping.arcana.tarot.deck.swordsCards
import com.schlepping.arcana.tarot.deck.wandsCards

object TarotDeck {

    val majorArcana: List<TarotCard> = majorArcanaCards
    val cups: List<TarotCard> = cupsCards
    val swords: List<TarotCard> = swordsCards
    val wands: List<TarotCard> = wandsCards
    val pentacles: List<TarotCard> = pentaclesCards

    val allCards: List<TarotCard> = majorArcana + cups + swords + wands + pentacles

    val validCardNames: Set<String> = allCards.mapTo(mutableSetOf()) { it.name }

    fun isValidCard(name: String): Boolean = name in validCardNames
}
