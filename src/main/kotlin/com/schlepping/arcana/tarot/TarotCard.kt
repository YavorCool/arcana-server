package com.schlepping.arcana.tarot

data class TarotCard(
    val id: Int,
    val name: String,
    val numeral: String,
    val suit: Suit,
    val keywords: List<String>,
    val uprightMeaning: String,
    val reversedMeaning: String,
)
