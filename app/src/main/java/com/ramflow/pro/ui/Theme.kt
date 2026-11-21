package com.ramflow.pro.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

val Blue = Color(0xFF2F7BFF)
val BlueLight = Color(0xFF6FB1FF)
val Green = Color(0xFF28C76F)
val Orange = Color(0xFFFF9F43)
val Red = Color(0xFFEA5455)

val LocalLang = staticCompositionLocalOf { "tr" }
val LocalAnim = staticCompositionLocalOf { true }

@Composable
fun tr(en: String, trk: String): String = if (LocalLang.current == "tr") trk else en

@Composable
fun isDarkTheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

@Composable
fun RamflowTheme(dark: Boolean, lang: String, anim: Boolean, content: @Composable () -> Unit) {
    val scheme = if (dark) darkColorScheme(
        primary = Blue, background = Color(0xFF0B1220), surface = Color(0xFF111A2B),
        onBackground = Color(0xFFF2F6FF), onSurface = Color(0xFFF2F6FF)
    ) else lightColorScheme(
        primary = Blue, background = Color(0xFFEAF3FF), surface = Color.White,
        onBackground = Color(0xFF0D1B2E), onSurface = Color(0xFF0D1B2E)
    )
    CompositionLocalProvider(LocalLang provides lang, LocalAnim provides anim) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
