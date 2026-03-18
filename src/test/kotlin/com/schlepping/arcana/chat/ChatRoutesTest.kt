package com.schlepping.arcana.chat

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.schlepping.arcana.FakeChatRepository
import com.schlepping.arcana.FakeLlmProvider
import com.schlepping.arcana.FakeSpreadRepository
import com.schlepping.arcana.auth.JwtClaims
import com.schlepping.arcana.llm.LlmException
import com.schlepping.arcana.llm.LlmPrompt
import com.schlepping.arcana.llm.LlmProvider
import com.schlepping.arcana.llm.LlmResponse
import com.schlepping.arcana.llm.prompt.PromptBuilder
import com.schlepping.arcana.llm.routing.LlmRouter
import com.schlepping.arcana.llm.routing.LlmRoutingConfig
import com.schlepping.arcana.plugins.ApiError
import com.schlepping.arcana.plugins.configureStatusPages
import com.schlepping.arcana.spread.CardData
import com.schlepping.arcana.spread.Reading
import com.schlepping.arcana.spread.SpreadType
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class ChatRoutesTest {

    private val jwtSecret = "test-secret"
    private val jwtIssuer = "arcana-server"
    private val jwtAudience = "arcana-app"

    private fun generateToken(
        deviceId: UUID,
        tier: String? = "free",
    ): String = JWT.create()
        .withIssuer(jwtIssuer)
        .withAudience(jwtAudience)
        .withClaim(JwtClaims.DEVICE_ID, deviceId.toString())
        .apply { if (tier != null) withClaim(JwtClaims.TIER, tier) }
        .withExpiresAt(java.util.Date(System.currentTimeMillis() + 60_000))
        .sign(Algorithm.HMAC256(jwtSecret))

    private fun createReading(deviceId: UUID): Reading = Reading(
        id = UUID.randomUUID(),
        deviceId = deviceId,
        spreadType = SpreadType.YES_NO,
        question = "Will I find love?",
        cards = listOf(CardData("The Fool", false)),
        interpretation = "The Fool speaks of new beginnings...",
        createdAt = LocalDateTime.now(ZoneOffset.UTC),
    )

    private fun Application.configureTestApp(
        fakeLlm: LlmProvider = FakeLlmProvider(),
        fakeSpreadRepo: FakeSpreadRepository = FakeSpreadRepository(),
        fakeChatRepo: FakeChatRepository = FakeChatRepository(),
    ) {
        install(ContentNegotiation) { json() }
        install(Authentication) {
            jwt("auth-jwt") {
                verifier(
                    JWT.require(Algorithm.HMAC256(jwtSecret))
                        .withAudience(jwtAudience)
                        .withIssuer(jwtIssuer)
                        .build(),
                )
                validate { credential ->
                    val deviceId = credential.payload.getClaim(JwtClaims.DEVICE_ID)?.asString()
                    if (credential.payload.audience.contains(jwtAudience) && deviceId != null) {
                        JWTPrincipal(credential.payload)
                    } else null
                }
                challenge { _, _ ->
                    call.respond(
                        status = HttpStatusCode.Unauthorized,
                        message = ApiError(error = "Token is not valid or has expired", code = "AUTH_ERROR"),
                    )
                }
            }
        }
        configureStatusPages()
        val routingConfig = LlmRoutingConfig(
            premiumReading = "gpt-5",
            freeReading = "gpt-5-mini",
            premiumChat = "gpt-5-mini",
            freeChat = "gpt-5-nano",
            dailyCard = "gpt-5-mini",
            firstReading = "gpt-5",
        )
        val service = ChatService(
            fakeChatRepo,
            fakeSpreadRepo,
            fakeLlm,
            LlmRouter(routingConfig),
            PromptBuilder(),
        )
        routing {
            chatRoutes(service)
        }
    }

    @Test
    fun `POST chat returns 201 with AI response`() = testApplication {
        val deviceId = UUID.randomUUID()
        val token = generateToken(deviceId)
        val fakeSpreadRepo = FakeSpreadRepository()
        val reading = createReading(deviceId)

        application {
            configureTestApp(fakeSpreadRepo = fakeSpreadRepo)
        }

        fakeSpreadRepo.saveReading(reading)

        val response = client.post("/api/v1/readings/${reading.id}/chat") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"message":"What does this mean?"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val body = Json.decodeFromString<ChatMessageResponse>(response.bodyAsText())
        assertEquals("Fake interpretation for your daily card.", body.aiResponse)
    }

    @Test
    fun `POST chat without JWT returns 401`() = testApplication {
        application { configureTestApp() }

        val response = client.post("/api/v1/readings/${UUID.randomUUID()}/chat") {
            contentType(ContentType.Application.Json)
            setBody("""{"message":"Hello"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST chat with invalid readingId returns 400`() = testApplication {
        val deviceId = UUID.randomUUID()
        val token = generateToken(deviceId)

        application { configureTestApp() }

        val response = client.post("/api/v1/readings/not-a-uuid/chat") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"message":"Hello"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST chat with empty message returns 400`() = testApplication {
        val deviceId = UUID.randomUUID()
        val token = generateToken(deviceId)
        val fakeSpreadRepo = FakeSpreadRepository()
        val reading = createReading(deviceId)

        application {
            configureTestApp(fakeSpreadRepo = fakeSpreadRepo)
        }

        fakeSpreadRepo.saveReading(reading)

        val response = client.post("/api/v1/readings/${reading.id}/chat") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"message":""}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST chat free tier limit returns 429`() = testApplication {
        val deviceId = UUID.randomUUID()
        val token = generateToken(deviceId)
        val fakeSpreadRepo = FakeSpreadRepository()
        val fakeChatRepo = FakeChatRepository()
        val reading = createReading(deviceId)

        application {
            configureTestApp(fakeSpreadRepo = fakeSpreadRepo, fakeChatRepo = fakeChatRepo)
        }

        fakeSpreadRepo.saveReading(reading)

        // First message succeeds
        client.post("/api/v1/readings/${reading.id}/chat") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"message":"First question"}""")
        }

        // Second message hits limit
        val response = client.post("/api/v1/readings/${reading.id}/chat") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"message":"Second question"}""")
        }

        assertEquals(HttpStatusCode.TooManyRequests, response.status)
        val body = Json.decodeFromString<ApiError>(response.bodyAsText())
        assertEquals("CHAT_LIMIT_REACHED", body.code)
    }

    @Test
    fun `POST chat to non-existent reading returns 404`() = testApplication {
        val deviceId = UUID.randomUUID()
        val token = generateToken(deviceId)

        application { configureTestApp() }

        val response = client.post("/api/v1/readings/${UUID.randomUUID()}/chat") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"message":"Hello"}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        val body = Json.decodeFromString<ApiError>(response.bodyAsText())
        assertEquals("READING_NOT_FOUND", body.code)
    }

    @Test
    fun `POST chat LLM error returns 503`() = testApplication {
        val deviceId = UUID.randomUUID()
        val token = generateToken(deviceId)
        val fakeSpreadRepo = FakeSpreadRepository()
        val reading = createReading(deviceId)
        val failingLlm = object : LlmProvider {
            override suspend fun generate(prompt: LlmPrompt): LlmResponse {
                throw LlmException("Service unavailable")
            }
        }

        application {
            configureTestApp(fakeLlm = failingLlm, fakeSpreadRepo = fakeSpreadRepo)
        }

        fakeSpreadRepo.saveReading(reading)

        val response = client.post("/api/v1/readings/${reading.id}/chat") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"message":"Hello"}""")
        }

        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
    }
}
