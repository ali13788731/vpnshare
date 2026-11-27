package ir.mrpc.hotshare.ui

import android.bluetooth.BluetoothDevice
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.mrpc.hotshare.ble.NearbyDevice
import ir.mrpc.hotshare.core.Offer

/** Every page is its own screen. Tabs (bottom bar) are only Home, Menu and Help; everything else opens from them. */
enum class Screen(val tab: Boolean = false) {
    SPLASH, HOME(true), MENU(true), HELP(true),
    HOST, RECEIVE, SCAN, CONNECT, ONBOARD,
    SETUP, SET_SHARE, SET_LOOK, SET_GENERAL, PRIVACY, ABOUT,
    HELP_WHICH, HELP_METHODS, HELP_STEPS, HELP_WIFI, HELP_FAQ
}

class Nav(
    val go: (Screen) -> Unit,
    val back: () -> Unit,
    val tab: (Screen) -> Unit
)

class Actions(
    val startHost: () -> Unit,
    val stopHost: () -> Unit,
    val connectDevice: (NearbyDevice, BluetoothDevice?) -> Unit,
    val connectOffer: (Offer) -> Unit,
    val connectCode: (String) -> Unit,
    val requestPerms: (Array<String>, (Boolean) -> Unit) -> Unit,
    val enableBluetooth: () -> Unit,
    val enableLocation: ((Boolean) -> Unit) -> Unit
)

/**
 * Fixed bottom bar. It sits *below* the screen content (not on top of it), so nothing can ever
 * slide under it, and it respects the phone's system navigation bar.
 */
@Composable
fun BottomNavBar(selected: Screen, onSelect: (Screen) -> Unit, modifier: Modifier = Modifier) {
    val hs = LocalHs.current
    val s = LocalStrings.current
    val items: List<Triple<Screen, ImageVector, String>> = listOf(
        Triple(Screen.HOME, Icons.Rounded.Home, s.navHome),
        Triple(Screen.MENU, Icons.Rounded.GridView, s.navMenu),
        Triple(Screen.HELP, Icons.Rounded.Info, s.navHelp)
    )
    val idx = items.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    Column(
        modifier.fillMaxWidth().background(if (hs.dark) Color(0xFF0B1228) else Color.White)
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(hs.cardStroke))
        BoxWithConstraints(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val itemW = maxWidth / items.size
            val x by animateDpAsState(itemW * idx, spring(dampingRatio = 0.7f, stiffness = 380f), label = "navx")
            Box(
                Modifier.offset(x = x).width(itemW).height(56.dp)
                    .clip(RoundedCornerShape(20.dp)).background(hs.brand)
            )
            Row {
                items.forEach { (scr, icon, label) ->
                    val sel = scr == selected
                    val tint by animateColorAsState(if (sel) Color.White else hs.sub, label = "navtint")
                    val sc by animateFloatAsState(if (sel) 1.1f else 1f, spring(0.5f, 400f), label = "navsc")
                    Column(
                        Modifier.width(itemW).height(56.dp).pressable { onSelect(scr) },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                    ) {
                        Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp).graphicsScale(sc))
                        Text(label, color = tint, fontSize = 11.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

private fun Modifier.graphicsScale(s: Float): Modifier = this.graphicsLayer(scaleX = s, scaleY = s)

@Composable
fun TopBar(title: String, onBack: (() -> Unit)?, tint: Color? = null, trailing: @Composable RowScope.() -> Unit = {}) {
    val hs = LocalHs.current
    val fg = tint ?: hs.text
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (onBack != null) {
            Box(
                Modifier.size(44.dp).pressable(onClick = onBack).clip(RoundedCornerShape(16.dp))
                    .background(if (tint != null) Color.Black.copy(alpha = 0.45f) else hs.card)
                    .border(1.dp, if (tint != null) Color.White.copy(alpha = 0.35f) else hs.cardStroke, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = fg) }
        }
        Text(title, color = fg, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        trailing()
    }
}
