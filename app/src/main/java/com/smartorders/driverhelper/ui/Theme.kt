package com.smartorders.driverhelper.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFF6B21A8)
val Purple60 = Color(0xFF9333EA)
val Purple40 = Color(0xFFBB86FC)
val Background = Color(0xFF0F172A)
val Surface = Color(0xFF1E293B)
val SurfaceVariant = Color(0xFF334155)
val OnSurface = Color(0xFFF8FAFC)
val OnSurfaceVariant = Color(0xFF94A3B8)
val GreenAccent = Color(0xFF22C55E)
val RedAccent = Color(0xFFEF4444)
val AmberAccent = Color(0xFFF59E0B)

private val DarkColorScheme = darkColorScheme(
    primary = Purple60,
    onPrimary = Color.White,
    primaryContainer = Purple80,
    onPrimaryContainer = Color.White,
    secondary = GreenAccent,
    onSecondary = Color.Black,
    background = Background,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    error = RedAccent,
    onError = Color.White
)

@Composable
fun SmartOrdersTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
