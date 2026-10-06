package com.poslik.caisse.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B5FFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6FF),
    onPrimaryContainer = Color(0xFF001A4D),
    secondary = Color(0xFF3A4A66),
    background = Color(0xFFF6F7FB),
    surface = Color.White,
    surfaceVariant = Color(0xFFE9ECF5),
    errorContainer = Color(0xFFFDE7E7),
    onErrorContainer = Color(0xFF5C0A0A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB3C6FF),
    onPrimary = Color(0xFF00296F),
    primaryContainer = Color(0xFF0B3FA8),
    onPrimaryContainer = Color(0xFFDCE6FF),
    secondary = Color(0xFFB8C4E0),
    background = Color(0xFF10131A),
    surface = Color(0xFF171B24),
    surfaceVariant = Color(0xFF252A36),
    errorContainer = Color(0xFF5C1A1A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val CaisseShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
)

/** Couleurs des états d'impression et de synchronisation, avec un contraste suffisant en clair comme en sombre. */
object StatusColors {
    val Success: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF6FD08C) else Color(0xFF1B873F)

    val Warning: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFFFFB74D) else Color(0xFFB26A00)

    val Error: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFFFF8A80) else Color(0xFFC62828)
}

@Composable
fun CaisseTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = CaisseShapes,
        content = content,
    )
}
