package com.foodzipp.app.auth

data class AuthResult(
    val success: Boolean,
    val message: String,
    val metadata: Map<String, String> = emptyMap()
)
