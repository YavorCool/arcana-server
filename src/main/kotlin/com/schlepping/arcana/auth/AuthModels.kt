package com.schlepping.arcana.auth

import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class RegisterRequest(
    val deviceId: String,
    val platform: String,
)

@Serializable
data class RefreshRequest(
    val refreshToken: String,
)

@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
)

data class Device(
    val deviceId: UUID,
    val platform: String,
    val tier: String,
)

data class StoredRefreshToken(
    val token: String,
    val deviceId: UUID,
    val expiresAt: LocalDateTime,
)