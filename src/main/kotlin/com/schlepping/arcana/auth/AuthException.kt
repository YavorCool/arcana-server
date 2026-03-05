package com.schlepping.arcana.auth

sealed class AuthException(message: String) : RuntimeException(message) {
    class InvalidRefreshToken : AuthException("Invalid refresh token")
    class ExpiredRefreshToken : AuthException("Refresh token expired")
}
