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

private val DarkColorScheme = darkColorScheme(
    primary = ProBlueLight,
    onPrimary = Color(0xFF002F65),
    primaryContainer = ProBlueDark,
    onPrimaryContainer = ProBlueContainer,
    secondary = ProSlateLight,
    onSecondary = Color(0xFF293041),
    secondaryContainer = ProSlateDark,
    onSecondaryContainer = ProBlueContainer,
    tertiary = ProPurpleLight,
    onTertiary = Color(0xFF402843),
    tertiaryContainer = Color(0xFF583E5B),
    onTertiaryContainer = Color(0xFFFBD7FC),
    background = SurfaceDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark,
    outlineVariant = Color(0xFF44474F),
    error = ProRedLight,
    onError = Color(0xFF690005)
)

private val LightColorScheme = lightColorScheme(
    primary = ProBlue,
    onPrimary = Color.White,
    primaryContainer = ProBlueContainer,
    onPrimaryContainer = ProBlueOnContainer,
    secondary = ProSlate,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E2EC),
    onSecondaryContainer = Color(0xFF141B2C),
    tertiary = ProPurple,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFBD7FC),
    onTertiaryContainer = Color(0xFF29132D),
    background = Color(0xFFFDFBFF),
    onBackground = TextPrimaryLight,
    surface = Color(0xFFFDFBFF),
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Color(0xFF44474F),
    outline = Color(0xFF74777F),
    outlineVariant = BorderLight,
    error = ProRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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

