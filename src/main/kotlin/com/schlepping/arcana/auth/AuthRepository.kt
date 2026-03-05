package com.schlepping.arcana.auth

import com.schlepping.arcana.db.Devices
import com.schlepping.arcana.db.RefreshTokens
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDateTime
import java.util.UUID

interface AuthRepository {
    suspend fun findDevice(deviceId: UUID): Device?
    suspend fun createDevice(deviceId: UUID, platform: String)
    suspend fun upsertDevice(deviceId: UUID, platform: String): Device
    suspend fun updateLastSeen(deviceId: UUID)
    suspend fun saveRefreshToken(token: String, deviceId: UUID, expiresAt: LocalDateTime)
    suspend fun findRefreshToken(token: String): StoredRefreshToken?
    suspend fun deleteRefreshToken(token: String)
    suspend fun deleteRefreshTokensByDevice(deviceId: UUID)
    suspend fun rotateRefreshToken(
        oldToken: String, newToken: String, deviceId: UUID, expiresAt: LocalDateTime,
    )
}

class AuthRepositoryImpl : AuthRepository {

    override suspend fun findDevice(deviceId: UUID): Device? = newSuspendedTransaction {
        Devices.selectAll().where { Devices.deviceId eq deviceId }
            .singleOrNull()
            ?.toDevice()
    }

    override suspend fun createDevice(deviceId: UUID, platform: String): Unit = newSuspendedTransaction {
        Devices.insert {
            it[Devices.deviceId] = deviceId
            it[Devices.platform] = platform
        }
    }

    override suspend fun upsertDevice(deviceId: UUID, platform: String): Device = newSuspendedTransaction {
        Devices.upsert(Devices.deviceId) {
            it[Devices.deviceId] = deviceId
            it[Devices.platform] = platform
            it[lastSeenAt] = LocalDateTime.now()
        }
        Devices.selectAll().where { Devices.deviceId eq deviceId }
            .single()
            .toDevice()
    }

    override suspend fun updateLastSeen(deviceId: UUID): Unit = newSuspendedTransaction {
        Devices.update({ Devices.deviceId eq deviceId }) {
            it[lastSeenAt] = LocalDateTime.now()
        }
    }

    override suspend fun saveRefreshToken(token: String, deviceId: UUID, expiresAt: LocalDateTime): Unit =
        newSuspendedTransaction {
            RefreshTokens.insert {
                it[RefreshTokens.token] = token
                it[RefreshTokens.deviceId] = deviceId
                it[RefreshTokens.expiresAt] = expiresAt
            }
        }

    override suspend fun findRefreshToken(token: String): StoredRefreshToken? = newSuspendedTransaction {
        RefreshTokens.selectAll().where { RefreshTokens.token eq token }
            .singleOrNull()
            ?.toStoredRefreshToken()
    }

    override suspend fun deleteRefreshToken(token: String): Unit = newSuspendedTransaction {
        RefreshTokens.deleteWhere { RefreshTokens.token eq token }
    }

    override suspend fun deleteRefreshTokensByDevice(deviceId: UUID): Unit = newSuspendedTransaction {
        RefreshTokens.deleteWhere { RefreshTokens.deviceId eq deviceId }
    }

    override suspend fun rotateRefreshToken(
        oldToken: String, newToken: String, deviceId: UUID, expiresAt: LocalDateTime,
    ): Unit = newSuspendedTransaction {
        RefreshTokens.deleteWhere { RefreshTokens.token eq oldToken }
        RefreshTokens.insert {
            it[RefreshTokens.token] = newToken
            it[RefreshTokens.deviceId] = deviceId
            it[RefreshTokens.expiresAt] = expiresAt
        }
    }

    private fun ResultRow.toDevice() = Device(
        deviceId = this[Devices.deviceId],
        platform = this[Devices.platform],
        tier = this[Devices.tier],
    )

    private fun ResultRow.toStoredRefreshToken() = StoredRefreshToken(
        token = this[RefreshTokens.token],
        deviceId = this[RefreshTokens.deviceId],
        expiresAt = this[RefreshTokens.expiresAt],
    )
}
