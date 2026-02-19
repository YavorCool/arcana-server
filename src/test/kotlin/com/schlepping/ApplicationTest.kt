package com.schlepping

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun testHealth() = testApplication {
        routing {
            get("/health") {
                call.respond(mapOf("status" to "ok"))
            }
        }
        client.get("/health").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }
}
