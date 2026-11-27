package ir.mrpc.hotshare.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.mrpc.hotshare.client.ConnFlow
import ir.mrpc.hotshare.client.Connector
import ir.mrpc.hotshare.core.Net
import kotlinx.coroutines.delay

/**
 * The phone is not on the host's hotspot. The app does not scan or list Wi-Fi networks (that is the phone's job):
 * it tells the user exactly which network to join, opens the phone's Wi-Fi settings, and keeps checking by itself.
 */
@Composable
fun WifiGuidePage(nw: ConnFlow.NeedWifi, onCancel: () -> Unit) {
    val s = LocalStrings.current
    val hs = LocalHs.current
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    var showPass by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) { if (copied) { delay(1500); copied = false } }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp).padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        IconBubble(Icons.Rounded.Wifi, Cyan, 72.dp)
        Text(s.wgTitle, color = hs.text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)

        // why we are here (never silent: the user always sees what is wrong)
        Text(s.wgReason(nw.reason, nw.expected, nw.current), color = hs.warnText, fontSize = 14.sp, lineHeight = 23.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)

        // the network to join
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(s.wgNetwork, color = hs.sub, fontSize = 12.sp)
                Text(nw.expected ?: s.wgUnknownName, color = hs.text, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                if (nw.expected == null) Text(s.wgAskName, color = hs.sub, fontSize = 12.sp, lineHeight = 19.sp, textAlign = TextAlign.Center)
                if (nw.psk != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(s.wgPassword, color = hs.sub, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            if (showPass) nw.psk else "•".repeat(nw.psk.length.coerceIn(6, 14)),
                            color = hs.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1
                        )
                        Box(Modifier.size(36.dp).pressable { showPass = !showPass }.clip(CircleShape).background(hs.cardStroke.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                            Icon(if (showPass) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null, tint = hs.text, modifier = Modifier.size(20.dp))
                        }
                        Box(Modifier.size(36.dp).pressable { clip.setText(AnnotatedString(nw.psk)); copied = true }.clip(CircleShape).background(hs.cardStroke.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.ContentCopy, null, tint = if (copied) Mint else hs.text, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // what to do, in order
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                s.wgSteps(nw.expected, nw.hasKey).forEachIndexed { i, step ->
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(26.dp).clip(CircleShape).background(hs.brand), contentAlignment = Alignment.Center) {
                            Text(s.n(i + 1), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(step, color = hs.text, fontSize = 14.sp, lineHeight = 24.sp, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        GradientButton(s.openWifiSettings, { Net.openWifiSettings(ctx) }, icon = Icons.Rounded.Wifi)
        if (nw.hasKey) GhostButton(s.wgRetryAuto, { Connector.retryWifiJoin() }, Modifier.fillMaxWidth(), Icons.Rounded.Refresh)

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CircularProgressIndicator(Modifier.size(16.dp), color = Cyan, strokeWidth = 2.dp)
            Text(s.wgWaiting, color = hs.sub, fontSize = 12.sp, lineHeight = 19.sp)
        }
        GhostButton(s.cancel, onCancel, Modifier.fillMaxWidth())
    }
}
