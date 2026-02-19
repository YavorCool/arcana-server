package com.schlepping.arcana.auth

import com.schlepping.arcana.db.Devices
import com.schlepping.arcana.db.RefreshTokens
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDateTime
import java.util.UUID

class AuthRepository {

    suspend fun findDevice(deviceId: UUID): Device? = newSuspendedTransaction {
        Devices.selectAll().where { Devices.deviceId eq deviceId }
            .singleOrNull()
            ?.let {
                Device(
                    deviceId = it[Devices.deviceId],
                    platform = it[Devices.platform],
                    tier = it[Devices.tier],
                )
            }
    }

    suspend fun createDevice(deviceId: UUID, platform: String): Unit = newSuspendedTransaction {
        Devices.insert {
            it[Devices.deviceId] = deviceId
            it[Devices.platform] = platform
        }
    }

    suspend fun updateLastSeen(deviceId: UUID): Unit = newSuspendedTransaction {
        Devices.update({ Devices.deviceId eq deviceId }) {
            it[lastSeenAt] = LocalDateTime.now()
        }
    }

    suspend fun saveRefreshToken(token: String, deviceId: UUID, expiresAt: LocalDateTime): Unit =
        newSuspendedTransaction {
            RefreshTokens.insert {
                it[RefreshTokens.token] = token
                it[RefreshTokens.deviceId] = deviceId
                it[RefreshTokens.expiresAt] = expiresAt
            }
        }

    suspend fun findRefreshToken(token: String): StoredRefreshToken? = newSuspendedTransaction {
        RefreshTokens.selectAll().where { RefreshTokens.token eq token }
            .singleOrNull()
            ?.let {
                StoredRefreshToken(
                    token = it[RefreshTokens.token],
                    deviceId = it[RefreshTokens.deviceId],
                    expiresAt = it[RefreshTokens.expiresAt],
                )
            }
    }

    suspend fun deleteRefreshToken(token: String): Unit = newSuspendedTransaction {
        RefreshTokens.deleteWhere { RefreshTokens.token eq token }
    }
}