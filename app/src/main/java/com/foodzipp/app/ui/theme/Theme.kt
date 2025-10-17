package com.foodzipp.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = FoodZippPrimary,
    onPrimary = FoodZippOnPrimary,
    surface = FoodZippSurface,
    onSurface = FoodZippOnSurface,
    secondary = Color(0xFF1E88E5),
    onSecondary = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = FoodZippPrimary,
    onPrimary = FoodZippOnPrimary,
    surface = Color(0xFF1B1B1F),
    onSurface = Color(0xFFF0F0F0)
)

@Composable
fun FoodZippTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
