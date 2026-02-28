package com.schlepping.arcana.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.schlepping.arcana.user.UserTier
import java.time.LocalDateTime
import java.util.UUID

class AuthService(
    private val repository: AuthRepository,
    private val jwtConfig: JwtConfig,
) {

    suspend fun register(deviceId: UUID, platform: String): TokenResponse {
        val device = repository.findDevice(deviceId)?.also {
            repository.updateLastSeen(deviceId)
        }
        if (device == null) {
            repository.createDevice(deviceId, platform)
        }

        val tier = device?.tier ?: UserTier.FREE.value
        return generateTokenPair(deviceId, tier)
    }

    suspend fun refresh(refreshToken: String): TokenResponse {
        val stored = repository.findRefreshToken(refreshToken)
            ?: throw IllegalArgumentException("Invalid refresh token")

        if (stored.expiresAt.isBefore(LocalDateTime.now())) {
            repository.deleteRefreshToken(refreshToken)
            throw IllegalArgumentException("Refresh token expired")
        }

        repository.deleteRefreshToken(refreshToken)
        repository.updateLastSeen(stored.deviceId)
        val device = repository.findDevice(stored.deviceId)
        val tier = device?.tier ?: UserTier.FREE.value
        return generateTokenPair(stored.deviceId, tier)
    }

    private suspend fun generateTokenPair(deviceId: UUID, tier: String = UserTier.FREE.value): TokenResponse {
        val accessToken = JWT.create()
            .withIssuer(jwtConfig.issuer)
            .withAudience(jwtConfig.audience)
            .withClaim(JwtClaims.DEVICE_ID, deviceId.toString())
            .withClaim(JwtClaims.TIER, tier)
            .withExpiresAt(java.util.Date(System.currentTimeMillis() + jwtConfig.accessTokenExpireMin * 60_000))
            .sign(Algorithm.HMAC256(jwtConfig.secret))

        val refreshToken = UUID.randomUUID().toString()
        val refreshExpiresAt = LocalDateTime.now().plusDays(jwtConfig.refreshTokenExpireDays)
        repository.saveRefreshToken(refreshToken, deviceId, refreshExpiresAt)

        return TokenResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
        )
    }
}
