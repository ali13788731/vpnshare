package ir.mrpc.hotshare.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.mrpc.hotshare.client.ConnFlow
import ir.mrpc.hotshare.client.Connector
import ir.mrpc.hotshare.core.Net
import ir.mrpc.hotshare.core.Perms
import ir.mrpc.hotshare.core.WifiNet
import ir.mrpc.hotshare.core.WifiScan
import kotlinx.coroutines.delay

/**
 * Shown by the connect screen whenever the phone is NOT on the host's Wi-Fi.
 * It explains why (red box), keeps scanning nearby networks by itself, and lets the user tap one.
 * When the host's hotspot name is known only that network is accepted; "Manual mode" is always available.
 */
@Composable
fun WifiPickPage(nw: ConnFlow.NeedWifi, nav: Nav, actions: Actions) {
    val s = LocalStrings.current
    val hs = LocalHs.current
    val ctx = LocalContext.current

    var hasPerm by remember { mutableStateOf(Perms.canScanWifi(ctx)) }
    val wifiOn by produceState(Net.wifiEnabled(ctx)) { while (true) { value = Net.wifiEnabled(ctx); delay(1500) } }
    val locOn by produceState(Net.locationOn(ctx)) { while (true) { value = Net.locationOn(ctx); delay(2000) } }
    var nets by remember { mutableStateOf(emptyList<WifiNet>()) }
    var seconds by remember { mutableIntStateOf(0) }
    var manual by remember { mutableStateOf(false) }
    var askPass by remember { mutableStateOf<String?>(null) }

    // results arrive by broadcast as soon as Android finishes a scan
    DisposableEffect(hasPerm, wifiOn) {
        val r = if (hasPerm && wifiOn) WifiScan.listen(ctx) { nets = WifiScan.read(ctx) } else null
        onDispose { r?.let { runCatching { ctx.applicationContext.unregisterReceiver(it) } } }
    }

    // automatic, continuous search while this page is visible
    LaunchedEffect(hasPerm, wifiOn) {
        seconds = 0
        if (!hasPerm || !wifiOn) { nets = emptyList(); return@LaunchedEffect }
        var tick = 0
        while (true) {
            if (tick % 4 == 0) WifiScan.kick(ctx)          // Android allows only a few scans per minute; cached results are still read
            nets = WifiScan.read(ctx)
            tick++; seconds += 2
            delay(2000)
        }
    }

    val rows = remember(nets, nw.expected) {
        buildList {
            val e = nw.expected
            if (e != null) add(nets.firstOrNull { it.ssid == e } ?: WifiNet(e, -100, true))
            addAll(nets.filter { it.ssid != e })
        }
    }

    fun pick(n: WifiNet) {
        when {
            // wrong network while the host's name is known: Connector refuses it and explains why
            nw.expected != null && n.ssid != nw.expected -> Connector.submitWifi(n.ssid, "")
            n.ssid == nw.expected && nw.hasKey -> Connector.submitWifi(n.ssid, "")
            !n.secured -> Connector.submitWifi(n.ssid, "")
            else -> askPass = n.ssid
        }
    }

    if (manual) WifiSheet(nw.expected, onSubmit = { a, b -> manual = false; Connector.submitWifi(a, b) }, onDismiss = { manual = false })
    askPass?.let { name -> WifiPasswordSheet(name, onSubmit = { p -> askPass = null; Connector.submitWifi(name, p) }, onDismiss = { askPass = null }) }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // why we are here
        val errShape = RoundedCornerShape(20.dp)
        Row(
            Modifier.fillMaxWidth().clip(errShape).background(Rose.copy(alpha = 0.14f)).border(1.dp, Rose.copy(alpha = 0.55f), errShape).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Rounded.ErrorOutline, null, tint = Rose, modifier = Modifier.size(28.dp))
            Text(s.wpReason(nw.reason, nw.expected, nw.current, nw.tried), color = hs.text, fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium)
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(s.wpNearby, color = hs.text, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            if (hasPerm && wifiOn) {
                CircularProgressIndicator(Modifier.size(14.dp), color = Cyan, strokeWidth = 2.dp)
                Text(s.wpAutoNote, color = hs.sub, fontSize = 12.sp)
            }
        }
        Text(if (nw.expected != null) s.wpOnlyHost else s.wpPickHost, color = hs.sub, fontSize = 13.sp, lineHeight = 19.sp)

        Box(Modifier.weight(1f).fillMaxWidth()) {
            val locHint = nets.isEmpty() && !locOn && seconds >= 6
            when {
                !hasPerm -> NoticeCard(s.wpPermNote, s.permGrant, null) {
                    actions.requestPerms(Perms.wifiScan()) { hasPerm = Perms.canScanWifi(ctx) }
                }
                !wifiOn -> NoticeCard(s.wifiOffNote, s.turnOnWifi, Icons.Rounded.Wifi) { Net.openWifiPanel(ctx) }
                rows.isEmpty() -> Column(
                    Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)
                ) {
                    CircularProgressIndicator(Modifier.size(34.dp), color = Cyan, strokeWidth = 3.dp)
                    Text(s.wpNone, color = hs.sub, fontSize = 14.sp, textAlign = TextAlign.Center)
                    if (locHint) {
                        Text(s.wpLocNote, color = Amber, fontSize = 13.sp, lineHeight = 19.sp, textAlign = TextAlign.Center)
                        GhostButton(s.wpLocBtn, { Net.openLocationSettings(ctx) }, Modifier.fillMaxWidth())
                    }
                }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 8.dp)) {
                    if (locHint) item(key = "loc") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(s.wpLocNote, color = Amber, fontSize = 13.sp, lineHeight = 19.sp)
                            GhostButton(s.wpLocBtn, { Net.openLocationSettings(ctx) }, Modifier.fillMaxWidth())
                        }
                    }
                    items(rows, key = { it.ssid }) { n ->
                        WifiRow(n, suggested = n.ssid == nw.expected) { pick(n) }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GhostButton(s.wpManual, { manual = true }, Modifier.weight(1f), Icons.Rounded.Edit)
            GhostButton(s.cancel, { Connector.cancel(); nav.back() }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun WifiRow(n: WifiNet, suggested: Boolean, onClick: () -> Unit) {
    val s = LocalStrings.current
    val hs = LocalHs.current
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().pressable(onClick = onClick).clip(shape)
            .background(if (suggested) Mint.copy(alpha = 0.14f) else hs.card)
            .border(if (suggested) 2.dp else 1.dp, if (suggested) Mint else hs.cardStroke, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SignalBars(n.bars)
        Column(Modifier.weight(1f)) {
            Text(n.ssid, color = hs.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (n.secured) s.wpSecured else s.wpOpenNet, color = hs.sub, fontSize = 12.sp)
        }
        if (suggested) {
            Text(
                s.wpSuggested, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(Mint).padding(horizontal = 10.dp, vertical = 5.dp)
            )
        } else if (n.secured) {
            Icon(Icons.Rounded.Lock, null, tint = hs.sub, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SignalBars(level: Int) {
    val hs = LocalHs.current
    Row(Modifier.height(22.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        listOf(7.dp, 12.dp, 17.dp, 22.dp).forEachIndexed { i, h ->
            Box(Modifier.width(5.dp).height(h).clip(RoundedCornerShape(2.dp)).background(if (i < level) Mint else hs.cardStroke))
        }
    }
}

@Composable
private fun NoticeCard(text: String, action: String, icon: androidx.compose.ui.graphics.vector.ImageVector?, onClick: () -> Unit) {
    val hs = LocalHs.current
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text, color = hs.text, fontSize = 14.sp, lineHeight = 21.sp)
            GradientButton(action, onClick, icon = icon)
        }
    }
}
