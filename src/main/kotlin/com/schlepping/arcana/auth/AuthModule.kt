package com.schlepping.arcana.auth

import io.ktor.server.application.*
import org.koin.dsl.module

fun Application.authModule() = module {
    single {
        JwtConfig(
            secret = environment.config.property("jwt.secret").getString(),
            issuer = environment.config.property("jwt.issuer").getString(),
            audience = environment.config.property("jwt.audience").getString(),
            accessTokenExpireMin = environment.config.property("jwt.accessTokenExpireMin").getString().toLong(),
            refreshTokenExpireDays = environment.config.property("jwt.refreshTokenExpireDays").getString().toLong(),
        )
    }
    single { AuthRepository() }
    single { AuthService(get(), get()) }
}
