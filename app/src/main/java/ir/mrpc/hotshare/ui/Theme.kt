package ir.mrpc.hotshare.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import ir.mrpc.hotshare.core.Prefs
import ir.mrpc.hotshare.core.Strings

val Cyan = Color(0xFF22D3EE)
val Violet = Color(0xFF8B5CF6)
val Good = Color(0xFF34D399)
val Warn = Color(0xFFFBBF24)
val Bad = Color(0xFFF87171)

val LocalStrings = staticCompositionLocalOf { Strings(true) }

enum class Screen { SPLASH, ROLE, HOST, CLIENT, SETTINGS, ABOUT }

@Composable
fun HotShareTheme(content: @Composable () -> Unit) {
    val dark = when (Prefs.theme) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val scheme = if (dark) darkColorScheme(
        primary = Cyan, secondary = Violet,
        background = Color(0xFF0B1020), surface = Color(0xFF141B34),
        onBackground = Color(0xFFE8ECFF), onSurface = Color(0xFFE8ECFF),
        onPrimary = Color(0xFF00252B)
    ) else lightColorScheme(
        primary = Color(0xFF0891B2), secondary = Violet,
        background = Color(0xFFF3F6FF), surface = Color.White,
        onBackground = Color(0xFF0B1020), onSurface = Color(0xFF0B1020)
    )
    val fa = Prefs.language == "fa"
    CompositionLocalProvider(
        LocalLayoutDirection provides if (fa) LayoutDirection.Rtl else LayoutDirection.Ltr,
        LocalStrings provides Strings(fa)
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
