package com.foodzipp.app.auth

interface AuthRepository {
    suspend fun sendOtpToPhone(phoneNumber: String): AuthResult
    suspend fun sendOtpToEmail(email: String): AuthResult
    suspend fun authenticateSocial(option: LoginOption, handle: String): AuthResult
    suspend fun verifyOtp(expectedOtp: String, providedOtp: String): AuthResult
}
