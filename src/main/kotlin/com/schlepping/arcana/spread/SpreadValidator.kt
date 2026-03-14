package com.schlepping.arcana.spread

import com.schlepping.arcana.tarot.TarotDeck

object SpreadValidator {

    fun validate(request: CreateReadingRequest) {
        val spreadType = SpreadType.fromString(request.spreadType)
            ?: throw IllegalArgumentException("Unknown spread type: ${request.spreadType}")

        if (spreadType == SpreadType.DAILY_CARD) {
            throw IllegalArgumentException("Use the daily card endpoint for daily readings")
        }

        if (request.cards.size != spreadType.requiredCardCount) {
            throw IllegalArgumentException(
                "Spread type ${spreadType.value} requires ${spreadType.requiredCardCount} card(s), " +
                    "but ${request.cards.size} provided",
            )
        }

        request.cards.forEach { card ->
            if (!TarotDeck.isValidCard(card.cardName)) {
                throw IllegalArgumentException("Unknown card: ${card.cardName}")
            }
        }

        val cardNames = request.cards.map { it.cardName }
        if (cardNames.size != cardNames.toSet().size) {
            throw IllegalArgumentException("Duplicate cards in spread")
        }

        request.question?.let {
            if (it.length > 500) {
                throw IllegalArgumentException("Question must be 500 characters or less")
            }
        }

        request.querentName?.let {
            if (it.length > 100) {
                throw IllegalArgumentException("Querent name must be 100 characters or less")
            }
        }
    }
}
