package com.schlepping.arcana.daily

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.schlepping.arcana.FakeDailyCardRepository
import com.schlepping.arcana.FakeLlmProvider
import com.schlepping.arcana.llm.*
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

class DailyCardRoutesTest {

    private val jwtSecret = "test-secret"
    private val jwtIssuer = "arcana-server"
    private val jwtAudience = "arcana-app"

    private fun generateToken(
        deviceId: UUID,
        tier: String? = "free",
    ): String = JWT.create()
        .withIssuer(jwtIssuer)
        .withAudience(jwtAudience)
        .withClaim("deviceId", deviceId.toString())
        .apply { if (tier != null) withClaim("tier", tier) }
        .withExpiresAt(java.util.Date(System.currentTimeMillis() + 60_000))
        .sign(Algorithm.HMAC256(jwtSecret))

    private fun Application.configureTestApp(
        fakeLlm: LlmProvider = FakeLlmProvider(),
        fakeRepo: DailyCardRepository = FakeDailyCardRepository(),
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
                    val deviceId = credential.payload.getClaim("deviceId")?.asString()
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
        val service = DailyCardService(fakeLlm, fakeRepo, LlmRouter(routingConfig), PromptBuilder())
        routing {
            dailyCardRoutes(service)
        }
    }

    @Test
    fun `POST daily with valid JWT returns 200 and DailyCardResponse`() = testApplication {
        val deviceId = UUID.randomUUID()
        val token = generateToken(deviceId)

        application { configureTestApp() }

        val response = client.post("/api/v1/readings/daily") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"cardName":"The Fool","isReversed":false}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.decodeFromString<DailyCardResponse>(response.bodyAsText())
        assertEquals("The Fool", body.cardName)
        assertEquals(false, body.isReversed)
        assertEquals(false, body.cached)
        assertTrue(body.interpretation.isNotEmpty())
        assertTrue(body.readingId.isNotEmpty())
    }

    @Test
    fun `POST daily without JWT returns 401`() = testApplication {
        application { configureTestApp() }

        val response = client.post("/api/v1/readings/daily") {
            contentType(ContentType.Application.Json)
            setBody("""{"cardName":"The Fool","isReversed":false}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST daily with JWT missing tier claim defaults to FREE`() = testApplication {
        val deviceId = UUID.randomUUID()
        val token = generateToken(deviceId, tier = null)
        val fakeLlm = FakeLlmProvider()

        application { configureTestApp(fakeLlm = fakeLlm) }

        val response = client.post("/api/v1/readings/daily") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"cardName":"The Star","isReversed":false}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val prompt = fakeLlm.lastPrompt!!
        assertTrue(prompt.userMessage.contains("brief", ignoreCase = true))
    }

    @Test
    fun `POST daily with invalid body returns error status`() = testApplication {
        val deviceId = UUID.randomUUID()
        val token = generateToken(deviceId)

        application { configureTestApp() }

        val response = client.post("/api/v1/readings/daily") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"invalid":"data"}""")
        }

        assertTrue(!response.status.isSuccess(), "Expected error status but got ${response.status}")
    }

    @Test
    fun `POST daily with LLM failure returns 503`() = testApplication {
        val deviceId = UUID.randomUUID()
        val token = generateToken(deviceId)
        val failingLlm = object : LlmProvider {
            override suspend fun generate(prompt: LlmPrompt): LlmResponse {
                throw LlmException("Service unavailable")
            }
        }

        application { configureTestApp(fakeLlm = failingLlm) }

        val response = client.post("/api/v1/readings/daily") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"cardName":"Death","isReversed":false}""")
        }

        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
    }
}
