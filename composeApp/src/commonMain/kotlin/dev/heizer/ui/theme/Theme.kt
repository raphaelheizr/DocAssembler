package dev.heizer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Palette: Modern Orange and Gray
val PrimaryOrange = Color(0xFFE67E22) // Elegant Orange
val SecondaryOrange = Color(0xFFD35400)
val DarkGray = Color(0xFF2C3E50)
val LightGray = Color(0xFFBDC3C7)
val Graphite = Color(0xFF455A64) // Graphite Gray
val BackgroundGray = Color(0xFFECF0F1)
val SurfaceWhite = Color(0xFFFFFFFF)
val InputBackground = Color(0xFFF2F2F2)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryOrange,
    onPrimary = Color.White,
    primaryContainer = PrimaryOrange.copy(alpha = 0.1f),
    onPrimaryContainer = SecondaryOrange,
    secondary = DarkGray,
    onSecondary = Color.White,
    background = BackgroundGray,
    surface = SurfaceWhite,
    onSurface = Graphite,
    surfaceVariant = LightGray.copy(alpha = 0.2f),
    onSurfaceVariant = Graphite
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryOrange,
    onPrimary = Color.White,
    secondary = LightGray,
    onSecondary = DarkGray,
    background = Color(0xFF1A1A1A),
    surface = Color(0xFF242424),
    onSurface = Color(0xFFD1D1D1) // Lighter graphite for dark theme
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 48.sp,
        letterSpacing = (-0.25).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun DocAssemblerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
