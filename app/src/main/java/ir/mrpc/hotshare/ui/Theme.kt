package ir.mrpc.hotshare.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ir.mrpc.hotshare.R
import ir.mrpc.hotshare.core.Prefs
import ir.mrpc.hotshare.core.Strings

val Cyan = Color(0xFF22D3EE)
val Violet = Color(0xFF8B5CF6)
val Pink = Color(0xFFEC4899)
val Mint = Color(0xFF34D399)
val Amber = Color(0xFFFBBF24)
val Rose = Color(0xFFFB7185)

@Immutable
class HsColors(
    val dark: Boolean,
    val bg: Color,
    val bg2: Color,
    val card: Color,
    val cardStroke: Color,
    val text: Color,
    val sub: Color,
    val brand: Brush
)

val LocalHs = compositionLocalOf<HsColors> { error("no theme") }
val LocalStrings = compositionLocalOf { Strings(false) }

private val Vazir = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
    Font(R.font.vazirmatn_bold, FontWeight.ExtraBold),
    Font(R.font.vazirmatn_bold, FontWeight.SemiBold)
)

private fun typo(f: FontFamily) = Typography().let { t ->
    fun s(x: TextStyle) = x.copy(fontFamily = f)
    Typography(
        displayLarge = s(t.displayLarge), displayMedium = s(t.displayMedium), displaySmall = s(t.displaySmall),
        headlineLarge = s(t.headlineLarge), headlineMedium = s(t.headlineMedium), headlineSmall = s(t.headlineSmall),
        titleLarge = s(t.titleLarge.copy(fontSize = 22.sp)), titleMedium = s(t.titleMedium), titleSmall = s(t.titleSmall),
        bodyLarge = s(t.bodyLarge), bodyMedium = s(t.bodyMedium), bodySmall = s(t.bodySmall),
        labelLarge = s(t.labelLarge), labelMedium = s(t.labelMedium), labelSmall = s(t.labelSmall)
    )
}

@Composable
fun HotShareTheme(content: @Composable () -> Unit) {
    val cfg = LocalConfiguration.current
    val fa = cfg.locales[0].language == "fa"
    val dark = when (Prefs.theme) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val hs = remember(dark) {
        if (dark) HsColors(
            true, Color(0xFF070B1A), Color(0xFF0E1530), Color.White.copy(alpha = 0.07f), Color.White.copy(alpha = 0.12f),
            Color(0xFFF1F5FF), Color(0xFF9AA6C7), Brush.linearGradient(listOf(Cyan, Violet, Pink))
        ) else HsColors(
            false, Color(0xFFF4F6FF), Color(0xFFE6EBFF), Color.White.copy(alpha = 0.85f), Color(0xFFD5DBF5),
            Color(0xFF0F1630), Color(0xFF5A6486), Brush.linearGradient(listOf(Color(0xFF0EA5C6), Color(0xFF7C3AED), Color(0xFFDB2777)))
        )
    }
    val scheme = if (dark) darkColorScheme(primary = Cyan, secondary = Violet, tertiary = Pink, background = hs.bg, surface = hs.bg2, onBackground = hs.text, onSurface = hs.text)
    else lightColorScheme(primary = Color(0xFF0EA5C6), secondary = Color(0xFF7C3AED), tertiary = Color(0xFFDB2777), background = hs.bg, surface = Color.White, onBackground = hs.text, onSurface = hs.text)
    val strings = remember(fa) { Strings(fa) }
    CompositionLocalProvider(LocalHs provides hs, LocalStrings provides strings) {
        MaterialTheme(colorScheme = scheme, typography = typo(if (fa) Vazir else FontFamily.Default), content = content)
    }
}
