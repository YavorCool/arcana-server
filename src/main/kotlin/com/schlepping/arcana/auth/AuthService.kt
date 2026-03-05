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
        val device = repository.upsertDevice(deviceId, platform)
        repository.deleteRefreshTokensByDevice(deviceId)
        return generateTokenPair(deviceId, device.tier)
    }

    suspend fun refresh(refreshToken: String): TokenResponse {
        val stored = repository.findRefreshToken(refreshToken)
            ?: throw AuthException.InvalidRefreshToken()

        if (stored.expiresAt.isBefore(LocalDateTime.now())) {
            repository.deleteRefreshToken(refreshToken)
            throw AuthException.ExpiredRefreshToken()
        }

        val device = repository.findDevice(stored.deviceId)
        val tier = device?.tier ?: UserTier.FREE.value

        val newRefreshToken = UUID.randomUUID().toString()
        val refreshExpiresAt = LocalDateTime.now().plusDays(jwtConfig.refreshTokenExpireDays)
        repository.rotateRefreshToken(refreshToken, newRefreshToken, stored.deviceId, refreshExpiresAt)
        repository.updateLastSeen(stored.deviceId)

        val accessToken = createAccessToken(stored.deviceId, tier)
        return TokenResponse(accessToken, newRefreshToken)
    }

    private fun createAccessToken(deviceId: UUID, tier: String = UserTier.FREE.value): String =
        JWT.create()
            .withIssuer(jwtConfig.issuer)
            .withAudience(jwtConfig.audience)
            .withClaim(JwtClaims.DEVICE_ID, deviceId.toString())
            .withClaim(JwtClaims.TIER, tier)
            .withExpiresAt(java.util.Date(System.currentTimeMillis() + jwtConfig.accessTokenExpireMin * 60_000))
            .sign(Algorithm.HMAC256(jwtConfig.secret))

    private suspend fun generateTokenPair(deviceId: UUID, tier: String = UserTier.FREE.value): TokenResponse {
        val accessToken = createAccessToken(deviceId, tier)
        val refreshToken = UUID.randomUUID().toString()
        val refreshExpiresAt = LocalDateTime.now().plusDays(jwtConfig.refreshTokenExpireDays)
        repository.saveRefreshToken(refreshToken, deviceId, refreshExpiresAt)
        return TokenResponse(accessToken = accessToken, refreshToken = refreshToken)
    }
}
