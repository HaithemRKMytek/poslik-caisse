package com.poslik.caisse.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PoslikColors = lightColorScheme(
    primary = Color(0xFF0B5FFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6FF),
    onPrimaryContainer = Color(0xFF001A4D),
    secondary = Color(0xFF3A4A66),
    background = Color(0xFFF6F7FB),
    surface = Color.White,
)

/** Couleurs des états d'impression et de synchronisation. */
object StatusColors {
    val Success = Color(0xFF1B873F)
    val Warning = Color(0xFFB26A00)
    val Error = Color(0xFFC62828)
}

@Composable
fun CaisseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PoslikColors, content = content)
}
