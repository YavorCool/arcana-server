package com.schlepping.arcana.auth

import com.schlepping.arcana.FakeAuthRepository
import com.schlepping.arcana.plugins.ApiError
import com.schlepping.arcana.plugins.configureStatusPages
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthRoutesTest {

    private fun Application.configureTestApp(
        fakeRepo: AuthRepository = FakeAuthRepository(),
    ) {
        install(ContentNegotiation) { json() }
        configureStatusPages()

        val service = AuthService(
            fakeRepo,
            JwtConfig(
                secret = "test-secret-key-at-least-32-chars-long",
                issuer = "arcana-server",
                audience = "arcana-app",
                accessTokenExpireMin = 15,
                refreshTokenExpireDays = 30,
            ),
        )
        routing {
            authRoutes(service)
        }
    }

    @Test
    fun `POST register with invalid deviceId returns 400`() = testApplication {
        application { configureTestApp() }

        val response = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"deviceId":"not-a-uuid","platform":"ios"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = Json.decodeFromString<ApiError>(response.bodyAsText())
        assertEquals("Invalid device ID format", body.error)
        assertEquals("BAD_REQUEST", body.code)
    }

    @Test
    fun `POST register with valid request returns 201`() = testApplication {
        application { configureTestApp() }

        val response = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"deviceId":"550e8400-e29b-41d4-a716-446655440000","platform":"ios"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `POST register with invalid JSON returns 400 INVALID_BODY`() = testApplication {
        application { configureTestApp() }

        val response = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("{broken")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = Json.decodeFromString<ApiError>(response.bodyAsText())
        assertEquals("INVALID_BODY", body.code)
    }
}
