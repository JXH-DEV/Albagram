package com.albagram.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Forest = Color(0xFF1A5F42)
val ForestDark = Color(0xFF0C3224)
val Leaf = Color(0xFF2F9E6F)
val Gold = Color(0xFFD4A84B)
val Cream = Color(0xFFF2F5F1)
val Ink = Color(0xFF1A2420)
val Mist = Color(0xFFD3E4DA)
val Coral = Color(0xFFB85C45)
val SoftSand = Color(0xFFE8EEE6)

private val LightColors = lightColorScheme(
    primary = Forest,
    onPrimary = Color.White,
    primaryContainer = Mist,
    onPrimaryContainer = ForestDark,
    secondary = Gold,
    onSecondary = Ink,
    secondaryContainer = Color(0xFFE8D7A4),
    onSecondaryContainer = Ink,
    tertiary = Leaf,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC5E6D4),
    onTertiaryContainer = ForestDark,
    background = Cream,
    onBackground = Ink,
    surface = SoftSand,
    onSurface = Ink,
    surfaceVariant = Mist,
    onSurfaceVariant = ForestDark,
    error = Coral,
    onError = Color.White,
    errorContainer = Color(0xFFF5D5CC),
    onErrorContainer = Color(0xFF5C2418),
    outline = Forest.copy(alpha = 0.35f),
    outlineVariant = Mist
)

private val DarkColors = darkColorScheme(
    primary = Leaf,
    onPrimary = ForestDark,
    primaryContainer = ForestDark,
    onPrimaryContainer = Mist,
    secondary = Gold,
    onSecondary = Ink,
    secondaryContainer = Color(0xFF4A3A16),
    onSecondaryContainer = Color(0xFFF0E2B0),
    tertiary = Color(0xFF7DCBA5),
    onTertiary = ForestDark,
    tertiaryContainer = Color(0xFF1E4A36),
    onTertiaryContainer = Mist,
    background = Color(0xFF0E1A14),
    onBackground = Cream,
    surface = Color(0xFF16241C),
    onSurface = Cream,
    surfaceVariant = Color(0xFF24362C),
    onSurfaceVariant = Mist,
    error = Color(0xFFE08A74),
    onError = Color(0xFF3A120C),
    errorContainer = Color(0xFF5C2418),
    onErrorContainer = Color(0xFFF5D5CC),
    outline = Mist.copy(alpha = 0.4f),
    outlineVariant = Color(0xFF2A3F34)
)

private val AlbagramTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 44.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp
    )
)

@Composable
fun AlbagramTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AlbagramTypography,
        content = content
    )
}
