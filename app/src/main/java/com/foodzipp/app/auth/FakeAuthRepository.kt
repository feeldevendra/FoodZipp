package com.foodzipp.app.auth

import kotlinx.coroutines.delay
import kotlin.random.Random

class FakeAuthRepository : AuthRepository {
    override suspend fun sendOtpToPhone(phoneNumber: String): AuthResult {
        delay(1_000)
        val otp = Random.nextInt(100000, 999999).toString()
        return AuthResult(
            success = true,
            message = "OTP sent to $phoneNumber",
            metadata = mapOf("otp" to otp)
        )
    }

    override suspend fun sendOtpToEmail(email: String): AuthResult {
        delay(1_000)
        val otp = Random.nextInt(100000, 999999).toString()
        return AuthResult(
            success = true,
            message = "Verification code emailed to $email",
            metadata = mapOf("otp" to otp)
        )
    }

    override suspend fun authenticateSocial(option: LoginOption, handle: String): AuthResult {
        delay(1_000)
        return AuthResult(
            success = true,
            message = "${option.displayName} authentication succeeded for $handle"
        )
    }

    override suspend fun verifyOtp(expectedOtp: String, providedOtp: String): AuthResult {
        delay(500)
        val success = expectedOtp == providedOtp
        return AuthResult(
            success = success,
            message = if (success) "OTP verified" else "Invalid OTP"
        )
    }
}
