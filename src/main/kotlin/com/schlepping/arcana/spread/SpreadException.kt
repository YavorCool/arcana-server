package com.schlepping.arcana.spread

sealed class SpreadException(message: String) : RuntimeException(message) {
    class DailyLimitReached : SpreadException("Daily spread limit reached. Try again tomorrow!")
}
