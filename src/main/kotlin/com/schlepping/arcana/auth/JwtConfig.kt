package com.schlepping.arcana.auth

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val accessTokenExpireMin: Long,
    val refreshTokenExpireDays: Long,
)