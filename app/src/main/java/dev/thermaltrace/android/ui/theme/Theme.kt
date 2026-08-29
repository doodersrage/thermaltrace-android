package dev.thermaltrace.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Matches thermaltrace.dev brand tokens
private val Navy = Color(0xFF090B0F)
private val Accent = Color(0xFF3B82F6)
private val AccentHover = Color(0xFF2563EB)
private val Text = Color(0xFFF8FAFC)
private val Muted = Color(0xFF94A3B8)
private val Success = Color(0xFF22C55E)
private val Danger = Color(0xFFF87171)
private val Surface = Color(0xFF111827)
private val SurfaceBright = Color(0xFF1F2937)

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Text,
    secondary = AccentHover,
    onSecondary = Text,
    background = Navy,
    onBackground = Text,
    surface = Surface,
    onSurface = Text,
    onSurfaceVariant = Muted,
    error = Danger,
    onError = Text,
    tertiary = Success,
)

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Text,
    secondary = AccentHover,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF64748B),
    error = Color(0xFFDC2626),
    tertiary = Success,
)

@Composable
fun ThermalTraceTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme || isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}
