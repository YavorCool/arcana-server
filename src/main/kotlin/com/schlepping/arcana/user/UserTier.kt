package com.schlepping.arcana.user

enum class UserTier(val value: String) {
    FREE("free"),
    PREMIUM("premium");

    companion object {
        fun fromString(s: String?): UserTier =
            entries.find { it.value == s?.lowercase() } ?: FREE
    }
}
