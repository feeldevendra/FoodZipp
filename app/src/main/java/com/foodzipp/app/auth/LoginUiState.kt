package com.foodzipp.app.auth

import android.graphics.Bitmap

data class LoginUiState(
    val selectedOption: LoginOption = LoginOption.MOBILE,
    val inputValue: String = "",
    val inputError: String? = null,
    val otpValue: String = "",
    val otpSent: Boolean = false,
    val isAuthenticating: Boolean = false,
    val statusMessage: String? = null,
    val termsAccepted: Boolean = false,
    val dynamicQr: Boolean = false,
    val qrUrl: String = "",
    val qrBitmap: Bitmap? = null,
    val loginComplete: Boolean = false,
    val analyticsEvents: List<Pair<String, Map<String, String>>> = emptyList()
) {
    val canSubmit: Boolean
        get() = inputError == null && inputValue.isNotBlank() && termsAccepted && !isAuthenticating
}
