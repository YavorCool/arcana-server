package com.schlepping.arcana.llm

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory

class OpenAiProvider(
    private val httpClient: HttpClient,
    private val config: OpenAiConfig,
) : LlmProvider {

    private val log = LoggerFactory.getLogger(OpenAiProvider::class.java)

    override suspend fun generate(prompt: LlmPrompt): LlmResponse {
        val request = OpenAiRequest(
            model = prompt.modelId,
            messages = listOf(
                OpenAiMessage(role = "system", content = prompt.systemMessage),
                OpenAiMessage(role = "user", content = prompt.userMessage),
            ),
        )

        var lastException: Exception? = null

        repeat(config.maxRetries + 1) { attempt ->
            try {
                val response = httpClient.post("${config.baseUrl}/chat/completions") {
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer ${config.apiKey}")
                    setBody(request)
                }

                if (response.status.isSuccess()) {
                    val body = response.body<OpenAiResponse>()
                    val content = body.choices.firstOrNull()?.message?.content
                        ?: throw LlmException("Empty response from OpenAI")

                    return LlmResponse(
                        content = content,
                        modelId = body.model,
                        promptTokens = body.usage.promptTokens,
                        completionTokens = body.usage.completionTokens,
                    )
                }

                val statusCode = response.status.value
                if (statusCode == 429 || statusCode in 500..599) {
                    lastException = LlmException("OpenAI returned $statusCode")
                    if (attempt < config.maxRetries) {
                        val delayMs = 1000L * (1 shl attempt) // 1s, 2s
                        log.warn("OpenAI returned $statusCode, retrying in ${delayMs}ms (attempt ${attempt + 1})")
                        delay(delayMs)
                        return@repeat
                    }
                } else {
                    throw LlmException("OpenAI returned $statusCode")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: LlmException) {
                throw e
            } catch (e: Exception) {
                lastException = e
                if (attempt < config.maxRetries) {
                    val delayMs = 1000L * (1 shl attempt)
                    log.warn("OpenAI request failed, retrying in ${delayMs}ms (attempt ${attempt + 1})", e)
                    delay(delayMs)
                } else {
                    throw LlmException("OpenAI request failed after ${config.maxRetries + 1} attempts", e)
                }
            }
        }

        throw LlmException("OpenAI request failed after ${config.maxRetries + 1} attempts", lastException)
    }
}
