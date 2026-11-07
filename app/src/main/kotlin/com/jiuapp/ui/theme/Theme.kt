package com.jiuapp.ui.theme

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

// Paleta mínima: uma cor de destaque e neutros. Cores dinâmicas (Android 12+) substituem a de destaque.
private val Accent = Color(0xFF1F4E79)

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CC7EB),
    onPrimary = Color(0xFF00305A),
)

@Composable
fun JiuAppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
