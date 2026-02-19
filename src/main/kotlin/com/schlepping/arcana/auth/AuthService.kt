package com.schlepping.arcana.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.time.LocalDateTime
import java.util.UUID

class AuthService(
    private val repository: AuthRepository,
    private val jwtConfig: JwtConfig,
) {

    suspend fun register(deviceId: UUID, platform: String): TokenResponse {
        repository.findDevice(deviceId)?.let {
            repository.updateLastSeen(deviceId)
        } ?: repository.createDevice(deviceId, platform)

        return generateTokenPair(deviceId)
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
        return generateTokenPair(stored.deviceId)
    }

    private suspend fun generateTokenPair(deviceId: UUID): TokenResponse {
        val accessToken = JWT.create()
            .withIssuer(jwtConfig.issuer)
            .withAudience(jwtConfig.audience)
            .withClaim("deviceId", deviceId.toString())
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
