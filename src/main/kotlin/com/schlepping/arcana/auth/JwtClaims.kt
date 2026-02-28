package com.schlepping.arcana.auth

import com.schlepping.arcana.user.UserTier
import io.ktor.server.auth.jwt.*
import java.util.UUID

object JwtClaims {
    const val DEVICE_ID = "deviceId"
    const val TIER = "tier"
}

fun JWTPrincipal.deviceId(): UUID =
    UUID.fromString(payload.getClaim(JwtClaims.DEVICE_ID).asString())

fun JWTPrincipal.userTier(): UserTier =
    UserTier.fromString(payload.getClaim(JwtClaims.TIER)?.asString())
