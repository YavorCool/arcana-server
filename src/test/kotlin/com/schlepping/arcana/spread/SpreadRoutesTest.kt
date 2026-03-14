package com.schlepping.arcana.spread

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.schlepping.arcana.FakeLlmProvider
import com.schlepping.arcana.FakeSpreadRepository
import com.schlepping.arcana.auth.JwtClaims
import com.schlepping.arcana.llm.*
import com.schlepping.arcana.llm.prompt.PromptBuilder
import com.schlepping.arcana.llm.routing.LlmRouter
import com.schlepping.arcana.llm.routing.LlmRoutingConfig
import com.schlepping.arcana.plugins.configureStatusPages
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
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpreadRoutesTest {

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

    private fun Application.configureTestApp(
        fakeLlm: LlmProvider = FakeLlmProvider(),
        fakeRepo: FakeSpreadRepository = FakeSpreadRepository(),
    ) {
        install(ContentNegotiation) { json() }
        install(Authentication) {
            jwt("auth-jwt") {
                verifier(
                    JWT.require(Algorithm.HMAC256(jwtSecret))
                        .withAudience(jwtAudience)
                        .withIssuer(jwtIssuer)
                        .build()
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
                        message = mapOf("error" to "Token is not valid or has expired"),
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
        val service = SpreadService(fakeLlm, fakeRepo, LlmRouter(routingConfig), PromptBuilder())
        routing {
            spreadRoutes(service)
        }
    }

    private fun yesNoBody(cardName: String = "The Fool", question: String? = null): String {
        val q = if (question != null) ""","question":"$question"""" else ""
        return """{"spreadType":"yes_no","cards":[{"cardName":"$cardName","isReversed":false}]$q}"""
    }

    // POST /api/v1/readings

    @Test
    fun `POST readings with valid JWT returns 201`() = testApplication {
        val deviceId = UUID.randomUUID()
        val token = generateToken(deviceId)

        application { configureTestApp() }

        val response = client.post("/api/v1/readings") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(yesNoBody())
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val body = Json.decodeFromString<CreateReadingResponse>(response.bodyAsText())
        assertTrue(body.readingId.isNotEmpty())
        assertTrue(body.interpretation.isNotEmpty())
    }

    @Test
    fun `POST readings without JWT returns 401`() = testApplication {
        application { configureTestApp() }

        val response = client.post("/api/v1/readings") {
            contentType(ContentType.Application.Json)
            setBody(yesNoBody())
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST readings with invalid card returns 400`() = testApplication {
        val deviceId = UUID.randomUUID()
        val token = generateToken(deviceId)

        application { configureTestApp() }

        val response = client.post("/api/v1/readings") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(yesNoBody(cardName = "The Joker"))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST readings exceeding daily limit returns 429`() = testApplication {
        val deviceId = UUID.randomUUID()
        val token = generateToken(deviceId)
        val fakeRepo = FakeSpreadRepository()
        fakeRepo.setTodaySpreads(deviceId, 1)

        application { configureTestApp(fakeRepo = fakeRepo) }

        val response = client.post("/api/v1/readings") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(yesNoBody())
        }

        assertEquals(HttpStatusCode.TooManyRequests, response.status)
    }

    @Test
    fun `POST readings with LLM failure returns 503`() = testApplication {
        val deviceId = UUID.randomUUID()
        val token = generateToken(deviceId)
        val failingLlm = object : LlmProvider {
            override suspend fun generate(prompt: LlmPrompt): LlmResponse {
                throw LlmException("Service unavailable")
            }
        }

        application { configureTestApp(fakeLlm = failingLlm) }

        val response = client.post("/api/v1/readings") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(yesNoBody())
        }

        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
    }

    // GET /api/v1/readings

    @Test
    fun `GET readings returns 200 with list`() = testApplication {
        val deviceId = UUID.randomUUID()
        val token = generateToken(deviceId)

        application { configureTestApp() }

        // Create a reading first
        client.post("/api/v1/readings") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(yesNoBody())
        }

        val response = client.get("/api/v1/readings") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.decodeFromString<ReadingsListResponse>(response.bodyAsText())
        assertEquals(1, body.readings.size)
        assertEquals(false, body.hasMore)
    }

    @Test
    fun `GET readings without JWT returns 401`() = testApplication {
        application { configureTestApp() }

        val response = client.get("/api/v1/readings")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    // GET /api/v1/readings/{readingId}

    @Test
    fun `GET reading detail returns 200`() = testApplication {
        val deviceId = UUID.randomUUID()
        val token = generateToken(deviceId)

        application { configureTestApp() }

        val createResponse = client.post("/api/v1/readings") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(yesNoBody())
        }
        val created = Json.decodeFromString<CreateReadingResponse>(createResponse.bodyAsText())

        val response = client.get("/api/v1/readings/${created.readingId}") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val detail = Json.decodeFromString<ReadingDetail>(response.bodyAsText())
        assertEquals(created.readingId, detail.readingId)
        assertEquals("yes_no", detail.spreadType)
    }

    @Test
    fun `GET reading detail for non-existent returns 400`() = testApplication {
        val deviceId = UUID.randomUUID()
        val token = generateToken(deviceId)

        application { configureTestApp() }

        val response = client.get("/api/v1/readings/${UUID.randomUUID()}") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET reading detail with invalid UUID returns 400`() = testApplication {
        val deviceId = UUID.randomUUID()
        val token = generateToken(deviceId)

        application { configureTestApp() }

        val response = client.get("/api/v1/readings/not-a-uuid") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET readings with limit query param returns correct page size`() = testApplication {
        val deviceId = UUID.randomUUID()
        val token = generateToken(deviceId)
        val fakeRepo = FakeSpreadRepository()

        application { configureTestApp(fakeRepo = fakeRepo) }

        // Create 3 readings
        repeat(3) {
            fakeRepo.setTodaySpreads(deviceId, 0)
            client.post("/api/v1/readings") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(yesNoBody())
            }
        }

        val response = client.get("/api/v1/readings?limit=2") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.decodeFromString<ReadingsListResponse>(response.bodyAsText())
        assertEquals(2, body.readings.size)
        assertTrue(body.hasMore)
    }
}
