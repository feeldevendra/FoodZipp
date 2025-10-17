package com.foodzipp.app.auth

enum class LoginOption(val displayName: String) {
    MOBILE("Mobile Number"),
    EMAIL("Email"),
    INSTAGRAM("Instagram"),
    FACEBOOK("Facebook");

    val placeholder: String
        get() = when (this) {
            MOBILE -> "+1 202 555 0199"
            EMAIL -> "user@example.com"
            INSTAGRAM -> "@username"
            FACEBOOK -> "facebook.id"
        }
}
