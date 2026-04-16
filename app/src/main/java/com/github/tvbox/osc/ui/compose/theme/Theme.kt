package com.github.tvbox.osc.ui.compose.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE50914), // Netflix Red
    onPrimary = Color.White,
    secondary = Color(0xFF564d4d),
    onSecondary = Color.White,
    background = Color(0xFF141414),
    onBackground = Color.White,
    surface = Color(0xFF141414),
    onSurface = Color.White,
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TVBoxTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
