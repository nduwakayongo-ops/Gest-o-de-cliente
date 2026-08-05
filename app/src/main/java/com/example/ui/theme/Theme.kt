package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = DeepBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F2FD),
    onPrimaryContainer = DeepBlueDark,
    secondary = ForestGreen,
    onSecondary = Color.White,
    secondaryContainer = LightGreen,
    onSecondaryContainer = ForestGreen,
    tertiary = DarkGold,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFFFF8E1),
    onTertiaryContainer = Color(0xFF5D4037),
    background = GrayBackground,
    onBackground = Color(0xFF1A202C),
    surface = Color.White,
    onSurface = Color(0xFF1A202C),
    surfaceVariant = GraySurface,
    onSurfaceVariant = GrayText,
    error = WarningRed,
    onError = Color.White,
    errorContainer = WarningRedLight,
    onErrorContainer = WarningRed
)

private val DarkColorScheme = darkColorScheme(
    primary = DeepBlueLight,
    onPrimary = Color.White,
    primaryContainer = DeepBlueDark,
    onPrimaryContainer = Color.White,
    secondary = ForestGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1B5E20),
    onSecondaryContainer = Color.White,
    tertiary = AccentGold,
    onTertiary = Color.Black,
    tertiaryContainer = DarkGold,
    onTertiaryContainer = Color.White,
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFB0BEC5),
    error = WarningRed,
    onError = Color.White
)

@Composable
fun GestaoClientesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set false to prioritize brand colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Alias for compatibility
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    GestaoClientesTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
