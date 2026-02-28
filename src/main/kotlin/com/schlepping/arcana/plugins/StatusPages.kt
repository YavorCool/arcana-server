package com.schlepping.arcana.plugins

import com.schlepping.arcana.llm.LlmException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable

@Serializable
data class ApiError(
    val error: String,
    val code: String,
)

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError(error = cause.message ?: "Bad request", code = "BAD_REQUEST"),
            )
        }

        exception<LlmException> { call, cause ->
            this@configureStatusPages.log.error("LLM error", cause)
            call.respond(
                HttpStatusCode.ServiceUnavailable,
                ApiError(error = "AI service temporarily unavailable", code = "LLM_ERROR"),
            )
        }

        exception<Throwable> { call, cause ->
            this@configureStatusPages.log.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiError(error = "Internal server error", code = "INTERNAL_ERROR"),
            )
        }
    }
}