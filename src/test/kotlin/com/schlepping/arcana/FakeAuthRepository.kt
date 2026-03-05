package com.schlepping.arcana

import com.schlepping.arcana.auth.AuthRepository
import com.schlepping.arcana.auth.Device
import com.schlepping.arcana.auth.StoredRefreshToken
import com.schlepping.arcana.user.UserTier
import java.time.LocalDateTime
import java.util.UUID

class FakeAuthRepository : AuthRepository {

    private val devices = mutableMapOf<UUID, Device>()
    private val tokens = mutableMapOf<String, StoredRefreshToken>()

    override suspend fun findDevice(deviceId: UUID): Device? = devices[deviceId]

    override suspend fun createDevice(deviceId: UUID, platform: String) {
        devices[deviceId] = Device(deviceId = deviceId, platform = platform, tier = UserTier.FREE.value)
    }

    override suspend fun upsertDevice(deviceId: UUID, platform: String): Device {
        val existing = devices[deviceId]
        val device = Device(
            deviceId = deviceId,
            platform = platform,
            tier = existing?.tier ?: UserTier.FREE.value,
        )
        devices[deviceId] = device
        return device
    }

    override suspend fun updateLastSeen(deviceId: UUID) {
        // no-op in fake
    }

    override suspend fun saveRefreshToken(token: String, deviceId: UUID, expiresAt: LocalDateTime) {
        tokens[token] = StoredRefreshToken(token = token, deviceId = deviceId, expiresAt = expiresAt)
    }

    override suspend fun findRefreshToken(token: String): StoredRefreshToken? = tokens[token]

    override suspend fun deleteRefreshToken(token: String) {
        tokens.remove(token)
    }

    override suspend fun deleteRefreshTokensByDevice(deviceId: UUID) {
        tokens.entries.removeAll { it.value.deviceId == deviceId }
    }

    override suspend fun rotateRefreshToken(
        oldToken: String, newToken: String, deviceId: UUID, expiresAt: LocalDateTime,
    ) {
        tokens.remove(oldToken)
        tokens[newToken] = StoredRefreshToken(token = newToken, deviceId = deviceId, expiresAt = expiresAt)
    }

    fun deviceCount(): Int = devices.size
    fun tokenCount(): Int = tokens.size
    fun allTokens(): List<StoredRefreshToken> = tokens.values.toList()

    fun clear() {
        devices.clear()
        tokens.clear()
    }
}
