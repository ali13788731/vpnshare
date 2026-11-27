package ir.mrpc.hotshare.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.mrpc.hotshare.client.ConnFlow
import ir.mrpc.hotshare.client.Connector
import ir.mrpc.hotshare.host.HostService

@Composable
fun HomeScreen(nav: Nav, actions: Actions) {
    val s = LocalStrings.current
    val hs = LocalHs.current
    val host by HostService.state.collectAsState()
    val flow by Connector.flow.collectAsState()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()
            .padding(horizontal = 20.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.padding(top = 18.dp).appear(0), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            BrandMark(52.dp)
            Column {
                Text("HotShare", color = hs.text, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text(s.tagline, color = hs.sub, fontSize = 13.sp)
            }
        }

        // Live sessions: they keep running when you leave the screen, so say so and offer the way to end them.
        if (host.running) {
            SessionCard(
                Mint, s.sharingNow(host.clients), s.sharingKeepsRunning,
                s.fixOpen, { nav.go(Screen.HOST) }, s.stopShort, { actions.stopHost() }, Modifier.appear(1)
            )
        }
        when (val f = flow) {
            is ConnFlow.Done -> SessionCard(
                Mint, s.connected + (f.host?.let { " — $it" } ?: ""), s.connectedKeepsRunning,
                s.fixOpen, { nav.go(Screen.CONNECT) }, s.disconnect, { Connector.disconnect() }, Modifier.appear(1)
            )
            is ConnFlow.Working, is ConnFlow.NeedWifi -> SessionCard(
                Cyan, s.connectingTo + "…", null,
                s.fixOpen, { nav.go(Screen.CONNECT) }, s.cancel, { Connector.cancel() }, Modifier.appear(1)
            )
            else -> {}
        }

        HeroCard(
            title = s.shareTitle, desc = s.shareDesc, icon = Icons.Rounded.Share,
            colors = listOf(Color(0xFF0891B2), Color(0xFF6366F1)), modifier = Modifier.appear(2)
        ) { nav.go(Screen.HOST) }

        HeroCard(
            title = s.receiveTitle, desc = s.receiveDesc, icon = Icons.Rounded.Download,
            colors = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)), modifier = Modifier.appear(3)
        ) { nav.go(Screen.RECEIVE) }
    }
}

@Composable
private fun SessionCard(
    color: Color, title: String, note: String?,
    openLabel: String, onOpen: () -> Unit, endLabel: String, onEnd: () -> Unit, modifier: Modifier
) {
    val hs = LocalHs.current
    GlassCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val t = rememberInfiniteTransition(label = "dot")
                val a by t.animateFloat(0.3f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "dota")
                Box(Modifier.size(12.dp).graphicsLayer { alpha = a }.clip(CircleShape).background(color))
                Text(title, color = hs.text, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 24.sp, modifier = Modifier.weight(1f))
            }
            if (note != null) Text(note, color = hs.sub, fontSize = 12.sp, lineHeight = 19.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton(openLabel, onOpen, Modifier.weight(1f).height(46.dp))
                GhostButton(endLabel, onEnd, Modifier.weight(1f).height(46.dp), tint = Rose)
            }
        }
    }
}

@Composable
private fun HeroCard(title: String, desc: String, icon: androidx.compose.ui.graphics.vector.ImageVector, colors: List<Color>, modifier: Modifier, onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "hero")
    val f by t.animateFloat(0f, 1f, infiniteRepeatable(tween(5200, easing = LinearEasing), RepeatMode.Reverse), label = "f")
    // Height follows the text (Persian text with the Vazirmatn font is taller than a fixed box and used to be cut off).
    Box(
        modifier.fillMaxWidth().pressable(onClick = onClick).clip(androidx.compose.foundation.shape.RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(colors))
    ) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width; val h = size.height
            drawCircle(Color.White.copy(alpha = 0.14f), h * 0.55f, Offset(w * (0.85f + 0.06f * f), h * (0.15f + 0.1f * f)))
            drawCircle(Color.White.copy(alpha = 0.10f), h * 0.36f, Offset(w * (0.62f - 0.05f * f), h * (1.0f - 0.12f * f)))
            drawCircle(Color.White.copy(alpha = 0.08f), h * 0.2f, Offset(w * (0.95f - 0.1f * f), h * 0.85f))
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 132.dp).padding(horizontal = 22.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, color = Color.White, fontSize = 24.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold)
                Text(desc, color = Color.White.copy(alpha = 0.92f), fontSize = 14.sp, lineHeight = 24.sp)
            }
            Spacer(Modifier.width(12.dp))
            Box(Modifier.size(60.dp).graphicsLayer { translationY = -6f * f }.clip(CircleShape).background(Color.White.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
        }
    }
}
