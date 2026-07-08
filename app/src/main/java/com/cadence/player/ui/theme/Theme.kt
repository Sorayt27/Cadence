package com.cadence.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Palette ───────────────────────────────────────────────────────────────
val Red          = Color(0xFFFC3C44)
val Black        = Color(0xFF000000)
val Surface      = Color(0xFF1C1C1E)
val Card         = Color(0xFF2C2C2E)
val Divider      = Color(0xFF3A3A3C)
val SecondaryTxt = Color(0xFF8E8E93)

private val ColorScheme = darkColorScheme(
    primary          = Red,
    onPrimary        = Color.White,
    background       = Black,
    onBackground     = Color.White,
    surface          = Surface,
    onSurface        = Color.White,
    surfaceVariant   = Card,
    onSurfaceVariant = SecondaryTxt,
    outline          = Divider,
    secondary        = SecondaryTxt,
    onSecondary      = Color.White,
    error            = Red,
)

@Composable
fun CadenceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        content     = content,
    )
}
