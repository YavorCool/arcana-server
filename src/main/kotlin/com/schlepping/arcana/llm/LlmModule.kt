package com.schlepping.arcana.llm

import com.schlepping.arcana.llm.openai.OpenAiConfig
import com.schlepping.arcana.llm.openai.OpenAiProvider
import com.schlepping.arcana.llm.prompt.PromptBuilder
import com.schlepping.arcana.llm.routing.LlmRouter
import com.schlepping.arcana.llm.routing.LlmRoutingConfig
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.events.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.koin.ktor.ext.get as koinGet

fun Application.llmModule() = module {
    single {
        val apiKey = environment.config.propertyOrNull("llm.openai.apiKey")?.getString() ?: ""
        require(apiKey.isNotBlank()) { "OPENAI_API_KEY environment variable must be set" }
        OpenAiConfig(
            apiKey = apiKey,
            baseUrl = environment.config.property("llm.openai.baseUrl").getString(),
            timeoutMs = environment.config.property("llm.openai.timeoutMs").getString().toLong(),
            maxRetries = environment.config.property("llm.openai.maxRetries").getString().toInt(),
        )
    }

    single {
        LlmRoutingConfig(
            premiumReading = environment.config.property("llm.routing.premiumReading").getString(),
            freeReading = environment.config.property("llm.routing.freeReading").getString(),
            premiumChat = environment.config.property("llm.routing.premiumChat").getString(),
            freeChat = environment.config.property("llm.routing.freeChat").getString(),
            dailyCard = environment.config.property("llm.routing.dailyCard").getString(),
            firstReading = environment.config.property("llm.routing.firstReading").getString(),
        )
    }

    single {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = get<OpenAiConfig>().timeoutMs
            }
        }
    }

    single<LlmProvider> { OpenAiProvider(get(), get()) }
    single { LlmRouter(get()) }
    single { PromptBuilder() }
}.also {
    monitor.subscribe(ApplicationStopped) {
        val httpClient = this@llmModule.koinGet<HttpClient>()
        httpClient.close()
    }
}
