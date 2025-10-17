package com.foodzipp.app.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodzipp.app.qr.QrGenerator
import java.util.UUID
import java.util.regex.Pattern
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val repository: AuthRepository = FakeAuthRepository(),
    private val analyticsTracker: InMemoryAnalyticsTracker = InMemoryAnalyticsTracker(),
    private val qrGenerator: QrGenerator = QrGenerator()
) : ViewModel() {

    companion object {
        private const val BASE_QR_URL = "https://auth.foodzipp.app/login?source=qr"
        private val PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{10,15}$")
        private val EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE)
        private val SOCIAL_PATTERN = Pattern.compile("^@?[A-Za-z0-9._]{2,30}$")
        private val FACEBOOK_PATTERN = Pattern.compile("^[A-Za-z0-9.]{3,50}$")
    }

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var expectedOtp: String? = null

    init {
        refreshQr(dynamic = false)
    }

    fun onSelectOption(option: LoginOption) {
        analyticsTracker.track("login_option_selected", mapOf("option" to option.name))
        _uiState.update {
            LoginUiState(
                selectedOption = option,
                dynamicQr = it.dynamicQr,
                qrUrl = it.qrUrl,
                qrBitmap = it.qrBitmap,
                termsAccepted = it.termsAccepted,
                analyticsEvents = analyticsTracker.events
            )
        }
    }

    fun onInputValueChange(value: String) {
        _uiState.update { state ->
            state.copy(
                inputValue = value,
                inputError = validateInput(state.selectedOption, value),
                statusMessage = null
            )
        }
    }

    fun onOtpValueChange(value: String) {
        _uiState.update { it.copy(otpValue = value.take(6)) }
    }

    fun onTermsAcceptedChange(accepted: Boolean) {
        _uiState.update { it.copy(termsAccepted = accepted) }
    }

    fun toggleDynamicQr() {
        refreshQr(dynamic = !_uiState.value.dynamicQr)
    }

    private fun refreshQr(dynamic: Boolean) {
        val url = if (dynamic) {
            "${BASE_QR_URL}&session=${UUID.randomUUID()}"
        } else {
            BASE_QR_URL
        }
        val bitmap = qrGenerator.generate(url)
        analyticsTracker.track(
            event = if (dynamic) "qr_generated_dynamic" else "qr_generated_static",
            attributes = mapOf("url" to url)
        )
        _uiState.update {
            it.copy(
                dynamicQr = dynamic,
                qrUrl = url,
                qrBitmap = bitmap,
                analyticsEvents = analyticsTracker.events
            )
        }
    }

    fun onCancelAuthentication() {
        analyticsTracker.track("authentication_cancelled")
        _uiState.update {
            LoginUiState(
                selectedOption = it.selectedOption,
                dynamicQr = it.dynamicQr,
                qrUrl = it.qrUrl,
                qrBitmap = it.qrBitmap,
                analyticsEvents = analyticsTracker.events
            )
        }
    }

    fun authenticate() {
        val state = _uiState.value
        if (!state.termsAccepted) {
            _uiState.update { it.copy(statusMessage = "Please accept the terms to continue") }
            return
        }
        if (state.inputError != null || state.inputValue.isBlank()) {
            _uiState.update { it.copy(statusMessage = state.inputError ?: "Enter a valid value") }
            return
        }
        when (state.selectedOption) {
            LoginOption.MOBILE -> handlePhoneFlow(state)
            LoginOption.EMAIL -> handleEmailFlow(state)
            LoginOption.INSTAGRAM, LoginOption.FACEBOOK -> handleSocialFlow(state)
        }
    }

    private fun handlePhoneFlow(state: LoginUiState) {
        if (!state.otpSent) {
            sendOtp(LoginOption.MOBILE, state.inputValue)
        } else if (state.otpValue.length == 6) {
            verifyOtp()
        } else {
            _uiState.update { it.copy(statusMessage = "Enter the 6-digit OTP") }
        }
    }

    private fun handleEmailFlow(state: LoginUiState) {
        if (!state.otpSent) {
            sendOtp(LoginOption.EMAIL, state.inputValue)
        } else if (state.otpValue.length == 6) {
            verifyOtp()
        } else {
            _uiState.update { it.copy(statusMessage = "Enter the 6-digit code") }
        }
    }

    private fun handleSocialFlow(state: LoginUiState) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthenticating = true, statusMessage = null) }
            analyticsTracker.track(
                "social_auth_started",
                mapOf("option" to state.selectedOption.name)
            )
            val result = repository.authenticateSocial(state.selectedOption, state.inputValue)
            _uiState.update {
                it.copy(
                    isAuthenticating = false,
                    statusMessage = result.message,
                    loginComplete = result.success,
                    analyticsEvents = analyticsTracker.events
                )
            }
            if (result.success) {
                analyticsTracker.track(
                    "social_auth_success",
                    mapOf("option" to state.selectedOption.name)
                )
                _uiState.update { it.copy(analyticsEvents = analyticsTracker.events) }
            }
        }
    }

    private fun sendOtp(option: LoginOption, value: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthenticating = true, statusMessage = null) }
            analyticsTracker.track("otp_requested", mapOf("option" to option.name))
            val result = when (option) {
                LoginOption.MOBILE -> repository.sendOtpToPhone(value)
                LoginOption.EMAIL -> repository.sendOtpToEmail(value)
                else -> return@launch
            }
            if (result.success) {
                expectedOtp = result.metadata["otp"]
            }
            _uiState.update {
                it.copy(
                    isAuthenticating = false,
                    statusMessage = result.message,
                    otpSent = result.success,
                    analyticsEvents = analyticsTracker.events
                )
            }
        }
    }

    private fun verifyOtp() {
        val expected = expectedOtp ?: run {
            _uiState.update { it.copy(statusMessage = "Request a new OTP") }
            return
        }
        val provided = _uiState.value.otpValue
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthenticating = true, statusMessage = null) }
            analyticsTracker.track("otp_verification_started")
            val result = repository.verifyOtp(expected, provided)
            if (result.success) {
                analyticsTracker.track("otp_verification_success")
            } else {
                analyticsTracker.track("otp_verification_failed")
            }
            _uiState.update {
                it.copy(
                    isAuthenticating = false,
                    statusMessage = result.message,
                    loginComplete = result.success,
                    analyticsEvents = analyticsTracker.events
                )
            }
        }
    }

    private fun validateInput(option: LoginOption, value: String): String? {
        if (value.isBlank()) return null
        return when (option) {
            LoginOption.MOBILE -> if (!PHONE_PATTERN.matcher(value).matches()) {
                "Enter a valid phone number"
            } else null
            LoginOption.EMAIL -> if (!EMAIL_PATTERN.matcher(value).matches()) {
                "Enter a valid email"
            } else null
            LoginOption.INSTAGRAM -> if (!SOCIAL_PATTERN.matcher(value).matches()) {
                "Enter a valid Instagram handle"
            } else null
            LoginOption.FACEBOOK -> if (!FACEBOOK_PATTERN.matcher(value).matches()) {
                "Enter a valid Facebook ID"
            } else null
        }
    }
}
