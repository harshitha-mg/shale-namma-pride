package com.example.shale.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OrangePrimary = Color(0xFFF97316)
private val BlueSecondary = Color(0xFF2563EB)

private val LightColorScheme = lightColorScheme(
    primary = OrangePrimary,
    secondary = BlueSecondary,
    background = Color(0xFFFDF8F0)
)

@Composable
fun ShaleNammaPrideTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
