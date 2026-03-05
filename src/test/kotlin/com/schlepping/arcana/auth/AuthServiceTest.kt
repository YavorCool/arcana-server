package com.schlepping.arcana.auth

import com.schlepping.arcana.FakeAuthRepository
import com.schlepping.arcana.user.UserTier
import kotlinx.coroutines.test.runTest
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthServiceTest {

    private val fakeRepo = FakeAuthRepository()
    private val jwtConfig = JwtConfig(
        secret = "test-secret-key-at-least-32-chars-long",
        issuer = "test-issuer",
        audience = "test-audience",
        accessTokenExpireMin = 15,
        refreshTokenExpireDays = 30,
    )
    private val service = AuthService(fakeRepo, jwtConfig)

    @Test
    fun `register with new deviceId creates device and returns tokens`() = runTest {
        val deviceId = UUID.randomUUID()

        val response = service.register(deviceId, "android")

        assertTrue(response.accessToken.isNotEmpty())
        assertTrue(response.refreshToken.isNotEmpty())
        assertNotNull(fakeRepo.findDevice(deviceId))
        assertEquals(1, fakeRepo.deviceCount())
    }

    @Test
    fun `register with existing deviceId upserts and returns new tokens`() = runTest {
        val deviceId = UUID.randomUUID()

        val first = service.register(deviceId, "android")
        val second = service.register(deviceId, "ios")

        assertEquals(1, fakeRepo.deviceCount())
        assertEquals("ios", fakeRepo.findDevice(deviceId)!!.platform)
        assertNotEquals(first.refreshToken, second.refreshToken)
    }

    @Test
    fun `register deletes old refresh tokens`() = runTest {
        val deviceId = UUID.randomUUID()

        service.register(deviceId, "android")
        assertEquals(1, fakeRepo.tokenCount())
        val firstToken = fakeRepo.allTokens().first().token

        service.register(deviceId, "android")
        assertEquals(1, fakeRepo.tokenCount())
        val secondToken = fakeRepo.allTokens().first().token

        assertNotEquals(firstToken, secondToken)
        assertNull(fakeRepo.findRefreshToken(firstToken))
    }

    @Test
    fun `refresh with valid token rotates tokens`() = runTest {
        val deviceId = UUID.randomUUID()
        val registered = service.register(deviceId, "android")

        val refreshed = service.refresh(registered.refreshToken)

        assertTrue(refreshed.accessToken.isNotEmpty())
        assertNotEquals(registered.refreshToken, refreshed.refreshToken)
        assertNull(fakeRepo.findRefreshToken(registered.refreshToken))
        assertNotNull(fakeRepo.findRefreshToken(refreshed.refreshToken))
    }

    @Test
    fun `refresh with expired token throws ExpiredRefreshToken`() = runTest {
        val deviceId = UUID.randomUUID()
        val expiredToken = "expired-token"
        fakeRepo.upsertDevice(deviceId, "android")
        fakeRepo.saveRefreshToken(expiredToken, deviceId, LocalDateTime.now().minusDays(1))

        assertFailsWith<AuthException.ExpiredRefreshToken> {
            service.refresh(expiredToken)
        }
        assertNull(fakeRepo.findRefreshToken(expiredToken))
    }

    @Test
    fun `refresh with non-existent token throws InvalidRefreshToken`() = runTest {
        assertFailsWith<AuthException.InvalidRefreshToken> {
            service.refresh("non-existent-token")
        }
    }
}
