package com.memecloud.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary               = CyberCyanDim,
    onPrimary             = Color.White,
    primaryContainer      = CyberCyanContainer,
    onPrimaryContainer    = CyberCyan,
    secondary             = NeonPurple,
    onSecondary           = Color.White,
    secondaryContainer    = Color(0xFF1A0E3D),
    onSecondaryContainer  = NeonPurpleLight,
    tertiary              = NeonRose,
    onTertiary            = Color.White,
    background            = LightBg,
    surface               = LightSurface,
    surfaceVariant        = LightCard,
    outline               = LightBorder,
    error                 = SemanticError,
    onBackground          = Color(0xFF111827),
    onSurface             = Color(0xFF111827),
    onSurfaceVariant      = Color(0xFF5A6480),
)

private val DarkColorScheme = darkColorScheme(
    primary               = CyberCyan,
    onPrimary             = Color(0xFF001F27),
    primaryContainer      = CyberCyanContainer,
    onPrimaryContainer    = CyberCyan,
    secondary             = NeonPurpleLight,
    onSecondary           = Color(0xFF1A0E3D),
    secondaryContainer    = Color(0xFF1A0E3D),
    onSecondaryContainer  = NeonPurpleLight,
    tertiary              = NeonRose,
    onTertiary            = Color.White,
    background            = DarkBg,
    surface               = DarkSurface,
    surfaceVariant        = DarkCard,
    outline               = DarkBorder,
    error                 = SemanticError,
    onBackground          = Color(0xFFE2E8F0),
    onSurface             = Color(0xFFE2E8F0),
    onSurfaceVariant      = Color(0xFF7A8BB0),
)

@Composable
fun MemeCloudTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = if (darkTheme) DarkBg.toArgb() else LightBg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
