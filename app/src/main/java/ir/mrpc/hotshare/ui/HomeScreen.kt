package ir.mrpc.hotshare.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.mrpc.hotshare.client.ConnFlow
import ir.mrpc.hotshare.client.Connector
import ir.mrpc.hotshare.core.Net
import ir.mrpc.hotshare.core.Prefs
import ir.mrpc.hotshare.host.HostService
import kotlinx.coroutines.delay

private class LiveStatus(val vpn: Boolean, val wifi: Boolean, val bt: Boolean)

@Composable
fun HomeScreen(nav: Nav, actions: Actions) {
    val s = LocalStrings.current
    val hs = LocalHs.current
    val ctx = LocalContext.current
    val coach = LocalCoach.current
    val status by produceState(LiveStatus(false, false, false)) {
        while (true) {
            value = LiveStatus(Net.isVpnActive(ctx), Net.isWifiConnected(ctx), Net.bluetoothOn(ctx))
            delay(1500)
        }
    }
    val wifiEnabled by produceState(Net.wifiEnabled(ctx)) { while (true) { value = Net.wifiEnabled(ctx); delay(1500) } }
    val host by HostService.state.collectAsState()
    val flow by Connector.flow.collectAsState()

    // first launch: walk the user through the screen once
    LaunchedEffect(Unit) {
        delay(1300)
        if (!Prefs.tourSeen("home")) coach.start("home")
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()
            .padding(horizontal = 20.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.padding(top = 18.dp).appear(0), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            BrandMark(52.dp)
            Column {
                Text("HotShare", color = hs.text, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text(s.tagline, color = hs.sub, fontSize = 13.sp)
            }
        }

        // live session banner
        val sessionText = when {
            host.running -> s.sharing to Mint
            flow is ConnFlow.Done -> s.connected to Mint
            flow is ConnFlow.Working || flow is ConnFlow.NeedWifi -> s.connectingTo + "…" to Cyan
            else -> null
        }
        if (sessionText != null) {
            GlassCard(Modifier.fillMaxWidth().appear(1), onClick = { nav.go(if (host.running) Screen.HOST else Screen.CONNECT) }) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val t = rememberInfiniteTransition(label = "dot")
                    val a by t.animateFloat(0.3f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "dota")
                    Box(Modifier.size(12.dp).graphicsLayer { alpha = a }.clip(CircleShape).background(sessionText.second))
                    Text(sessionText.first, color = hs.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        // something isn't ready (Bluetooth / Wi-Fi off) -> one tap to the fix-it screen
        if (!status.bt || !wifiEnabled) {
            GlassCard(Modifier.fillMaxWidth().appear(1), onClick = { nav.go(Screen.SETUP) }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    IconBubble(Icons.Rounded.Settings, Amber, 44.dp)
                    Column(Modifier.weight(1f)) {
                        Text(s.homeNotReady, color = hs.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(s.homeNotReadyDesc, color = hs.sub, fontSize = 12.sp, lineHeight = 17.sp)
                    }
                }
            }
        }

        Text(s.chooseRole, color = hs.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.appear(2))

        HeroCard(
            title = s.shareTitle, desc = s.shareDesc, icon = Icons.Rounded.Share,
            colors = listOf(Color(0xFF0891B2), Color(0xFF6366F1)), modifier = Modifier.coachTarget("home_share").appear(3)
        ) { nav.go(Screen.HOST) }

        HeroCard(
            title = s.receiveTitle, desc = s.receiveDesc, icon = Icons.Rounded.Download,
            colors = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)), modifier = Modifier.coachTarget("home_receive").appear(4)
        ) { nav.go(Screen.RECEIVE) }

        Text(s.statusTitle, color = hs.sub, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.appear(5))
        Row(Modifier.fillMaxWidth().coachTarget("home_status").appear(6), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusChip(
                Icons.Rounded.VpnKey, s.chipVpn, if (status.vpn) s.active else s.inactive, status.vpn, Modifier.weight(1f),
                onClick = if (status.vpn) null else ({ Net.openVpnSettings(ctx) })
            )
            StatusChip(
                Icons.Rounded.Wifi, s.chipWifi, if (wifiEnabled) s.on else s.off, wifiEnabled, Modifier.weight(1f),
                onClick = if (wifiEnabled) null else ({ Net.openWifiPanel(ctx) })
            )
            StatusChip(
                Icons.Rounded.Bluetooth, s.chipBluetooth, if (status.bt) s.on else s.off, status.bt, Modifier.weight(1f),
                onClick = if (status.bt) null else ({ actions.enableBluetooth() })
            )
        }
        Text(s.chipHint, color = hs.sub, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(horizontal = 4.dp).appear(7))

        GhostButton(s.tourStart, { nav.tour("home") }, Modifier.fillMaxWidth().appear(8), Icons.Rounded.Lightbulb, Amber)
    }
}

@Composable
private fun HeroCard(title: String, desc: String, icon: androidx.compose.ui.graphics.vector.ImageVector, colors: List<Color>, modifier: Modifier, onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "hero")
    val f by t.animateFloat(0f, 1f, infiniteRepeatable(tween(5200, easing = LinearEasing), RepeatMode.Reverse), label = "f")
    Box(
        modifier.fillMaxWidth().height(150.dp).pressable(onClick = onClick).clip(androidx.compose.foundation.shape.RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(colors))
    ) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width; val h = size.height
            drawCircle(Color.White.copy(alpha = 0.14f), h * 0.55f, Offset(w * (0.85f + 0.06f * f), h * (0.15f + 0.1f * f)))
            drawCircle(Color.White.copy(alpha = 0.10f), h * 0.36f, Offset(w * (0.62f - 0.05f * f), h * (1.0f - 0.12f * f)))
            drawCircle(Color.White.copy(alpha = 0.08f), h * 0.2f, Offset(w * (0.95f - 0.1f * f), h * 0.85f))
        }
        Row(Modifier.fillMaxSize().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text(desc, color = Color.White.copy(alpha = 0.88f), fontSize = 14.sp, lineHeight = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Box(Modifier.size(64.dp).graphicsLayer { translationY = -6f * f }.clip(CircleShape).background(Color.White.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
    }
}
