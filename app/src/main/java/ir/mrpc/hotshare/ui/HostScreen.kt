package ir.mrpc.hotshare.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.mrpc.hotshare.client.ConnFlow
import ir.mrpc.hotshare.client.Connector
import ir.mrpc.hotshare.core.Net
import ir.mrpc.hotshare.core.Pairing
import ir.mrpc.hotshare.core.Perms
import ir.mrpc.hotshare.core.Prefs
import ir.mrpc.hotshare.host.ApMode
import ir.mrpc.hotshare.host.BleState
import ir.mrpc.hotshare.host.HostService
import kotlinx.coroutines.delay

/** What the phone itself says right now (used before sharing has started). */
private class PreStatus(val hotspot: Boolean, val vpn: Boolean, val ble: BleState)

private fun preBle(ctx: android.content.Context): BleState = when {
    !Net.bleAdvertiseSupported(ctx) -> BleState.UNSUPPORTED
    !Perms.has(ctx, Perms.bleHost()) -> BleState.NO_PERMISSION
    !Net.bluetoothOn(ctx) -> BleState.BT_OFF
    else -> BleState.ON
}

private enum class Level { OK, WARN, INFO }

@Composable
fun HostScreen(nav: Nav, actions: Actions) {
    val s = LocalStrings.current
    val hs = LocalHs.current
    val ctx = LocalContext.current
    val info by HostService.state.collectAsState()
    val flow by Connector.flow.collectAsState()
    val pre by produceState(PreStatus(false, false, BleState.BT_OFF)) {
        while (true) {
            value = PreStatus(Net.hotspotIp() != null, Net.isVpnActive(ctx), preBle(ctx))
            delay(1200)
        }
    }
    LaunchedEffect(Unit) {
        delay(600)
        if (Prefs.autoStart && !HostService.state.value.running && Net.hotspotIp() != null && !Connector.isBusy()) actions.startHost()
    }

    val receivingOwn = flow is ConnFlow.Done || flow is ConnFlow.Working || flow is ConnFlow.NeedWifi
    val hotspotOk = info.ap != ApMode.NONE || (!info.running && pre.hotspot)
    val vpnOk = !receivingOwn && (if (info.running) info.vpn else pre.vpn)
    val ble = if (info.running) info.bleState else pre.ble

    var showAp by remember { mutableStateOf(false) }
    if (showAp) HotspotDetailsSheet(Prefs.apSsid, Prefs.apPsk, { a, b -> Prefs.apSsid = a; Prefs.apPsk = b; Prefs.save(); showAp = false }, { showAp = false })

    var tab by remember { mutableStateOf("status") }

    // announce a newly connected phone at the very top, whatever tab is open
    val seen = remember { mutableSetOf<String>().apply { addAll(HostService.state.value.devices.map { it.ip }) } }
    var joined by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(info.devices) {
        seen.retainAll(info.devices.map { it.ip }.toSet())
        val fresh = info.devices.firstOrNull { seen.add(it.ip) }
        if (fresh != null) { joined = fresh.name; delay(4500); joined = null }
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(s.hostTitle, onBack = nav.back)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // hero orb (compact)
            Box(Modifier.fillMaxWidth().height(if (info.running) 120.dp else 110.dp).appear(0), contentAlignment = Alignment.Center) {
                if (info.running) PulseRings(Modifier.size(120.dp), Mint) else PulseRings(Modifier.size(110.dp), Violet, rings = 2, periodMs = 5200)
                OrbitRings(Modifier.size(92.dp), speed = if (info.running) 1.6f else 0.6f)
                BrandMark(52.dp)
            }

            // who is connected: always visible at the top while sharing
            if (info.running) {
                AnimatedVisibility(visible = joined != null) {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = Mint, modifier = Modifier.size(24.dp))
                            Text(s.deviceJoined(joined ?: ""), color = hs.okText, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
                GlassCard(Modifier.fillMaxWidth().appear(1), onClick = { tab = "devices" }) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        IconBubble(Icons.Rounded.People, if (info.clients > 0) Mint else Cyan, 44.dp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(if (info.clients > 0) s.devicesConnected(info.clients) else s.waitingDevices, color = hs.text, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 22.sp)
                            if (info.clients > 0) Text(info.devices.joinToString("، ") { it.name }, color = hs.sub, fontSize = 12.sp, maxLines = 2, lineHeight = 18.sp)
                        }
                    }
                }
            }

            // one section at a time
            val tabs = listOf(
                "status" to s.tabStatus,
                "connect" to s.tabConnect,
                "devices" to (s.tabDevices + if (info.clients > 0) " (${s.n(info.clients)})" else "")
            )
            Segmented(tabs, tab) { tab = it }

            when (tab) {
                "status" -> StatusTab(info.running, info.error, ble, hotspotOk, vpnOk, receivingOwn, info.ap, info.apFailed, ctx, actions, info.ssid, { showAp = true })
                "connect" -> ConnectTab(actions, info.running, ble)
                else -> DevicesTab(info.running)
            }
        }

        // start / stop is always in reach
        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 12.dp, top = 4.dp)) {
            if (info.running) GhostButton(s.stopShare, { actions.stopHost() }, Modifier.fillMaxWidth(), Icons.Rounded.PowerSettingsNew, hs.errText)
            else GradientButton(s.startShare, { actions.startHost() }, Modifier, Icons.Rounded.PlayArrow, enabled = !receivingOwn)
        }
    }
}

/* ───────────── Tab 1: setup / status ───────────── */

@Composable
private fun StatusTab(
    running: Boolean, error: String?, ble: BleState, hotspotOk: Boolean, vpnOk: Boolean, receivingOwn: Boolean,
    ap: ApMode, apFailed: Boolean, ctx: android.content.Context, actions: Actions, ssid: String?, onEditAp: () -> Unit
) {
    val s = LocalStrings.current
    val hs = LocalHs.current
    val hasWifi = Net.hasWifiHardware(ctx)

    if (error != null) {
        GlassCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.ErrorOutline, null, tint = Rose, modifier = Modifier.size(26.dp))
                Text(s.hostPortBusy, color = hs.errText, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f))
            }
        }
    }

    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            // Hotspot
            CheckItem(
                Icons.Rounded.WifiTethering, s.stepHotspot,
                when {
                    !hasWifi -> s.noWifiHw
                    ap == ApMode.LOCAL -> s.hotspotAuto
                    hotspotOk -> s.hotspotSystem
                    running && !apFailed && Prefs.autoHotspot -> s.hotspotWaiting
                    else -> s.hotspotOff
                },
                when { !hasWifi -> Level.INFO; hotspotOk -> Level.OK; else -> Level.WARN },
                s.openHotspot, { Net.openHotspotSettings(ctx) }
            )
            // VPN
            CheckItem(
                Icons.Rounded.VpnKey, s.stepVpn,
                when { receivingOwn -> s.ownVpnBusy; vpnOk -> s.vpnOn; else -> s.vpnOff },
                if (vpnOk) Level.OK else Level.WARN,
                if (receivingOwn) s.disconnect else s.fixOpen,
                { if (receivingOwn) Connector.disconnect() else Net.openVpnSettings(ctx) }
            )
            // Bluetooth visibility: each situation has its own wording and its own fix button
            when (ble) {
                BleState.UNSUPPORTED -> CheckItem(Icons.Rounded.Visibility, s.stepVisible, s.bleUnsupportedShort, Level.INFO, null, null, note = s.bleUnsupported)
                BleState.NO_PERMISSION -> CheckItem(Icons.Rounded.Visibility, s.stepVisible, s.bleNoPerm, Level.WARN, s.fixGrant, { actions.requestPerms(Perms.bleHost()) {} })
                BleState.BT_OFF -> CheckItem(Icons.Rounded.Visibility, s.stepVisible, s.bleOffState, Level.WARN, s.fixTurnOn, { actions.enableBluetooth() })
                BleState.STARTING -> CheckItem(Icons.Rounded.Visibility, s.stepVisible, s.bleStarting, Level.WARN, null, null)
                BleState.ON -> CheckItem(Icons.Rounded.Visibility, s.stepVisible, s.visibleOn, Level.OK, null, null)
            }
        }
    }

    // one short hint at a time
    when {
        !vpnOk && !receivingOwn -> Text(s.vpnNote, color = hs.warnText, fontSize = 13.sp, lineHeight = 21.sp, modifier = Modifier.padding(horizontal = 6.dp))
        !hotspotOk && hasWifi && (!Prefs.autoHotspot || apFailed) ->
            Text(s.hotspotNote, color = hs.sub, fontSize = 13.sp, lineHeight = 21.sp, modifier = Modifier.padding(horizontal = 6.dp))
    }

    // hotspot name + password: the receiver is shown this name and can join automatically
    val name = ssid ?: Prefs.apSsid.takeIf { it.isNotEmpty() }
    if (ap != ApMode.LOCAL) {
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBubble(Icons.Rounded.Wifi, if (name != null) Mint else Amber, 40.dp)
                    Column(Modifier.weight(1f)) {
                        Text(s.hostHotspotName, color = hs.sub, fontSize = 12.sp)
                        Text(name ?: s.hostHotspotNotSet, color = hs.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    Box(
                        Modifier.pressable { onEditAp() }.clip(CircleShape).background(hs.brand).padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(s.hostHotspotEdit, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                }
                if (name == null || Prefs.apPsk.isEmpty()) Text(s.hostHotspotHint, color = hs.sub, fontSize = 12.sp, lineHeight = 19.sp)
            }
        }
    }
}

/* ───────────── Tab 2: QR + code ───────────── */

@Composable
private fun ConnectTab(actions: Actions, running: Boolean, ble: BleState) {
    val s = LocalStrings.current
    val hs = LocalHs.current
    val ctx = LocalContext.current
    val info by HostService.state.collectAsState()

    if (ble == BleState.UNSUPPORTED) {
        Text(s.codeOnlyNote, color = hs.warnText, fontSize = 13.sp, lineHeight = 21.sp, modifier = Modifier.padding(horizontal = 6.dp))
    }

    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (running) {
                val offer = remember(info.ip, info.ssid, info.psk, Prefs.code, Prefs.apSsid, Prefs.apPsk) { HostService.currentOffer(ctx) }
                val uri = remember(offer) { Pairing.toUri(offer) }
                Box(Modifier.clip(RoundedCornerShape(28.dp)).background(Color.White).padding(14.dp)) {
                    QrCodeView(uri, Modifier.size(220.dp)) {
                        Icon(Icons.Rounded.Shield, null, tint = Violet, modifier = Modifier.size(26.dp))
                    }
                }
                Text(s.scanHint, color = hs.sub, fontSize = 13.sp, textAlign = TextAlign.Center)
            } else {
                Icon(Icons.Rounded.QrCode2, null, tint = hs.sub, modifier = Modifier.size(56.dp))
                Text(s.qrAfterStart, color = hs.sub, fontSize = 13.sp, lineHeight = 21.sp, textAlign = TextAlign.Center)
            }
        }
    }

    GlassCard(Modifier.fillMaxWidth()) {
        val clip = LocalClipboardManager.current
        var copied by remember { mutableStateOf(false) }
        LaunchedEffect(copied) { if (copied) { delay(1500); copied = false } }
        Column(Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(s.codeLabel, color = hs.sub, fontSize = 13.sp)
            DigitBoxes(Prefs.code, boxWidth = 30.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                GhostButton(if (copied) s.copied else s.copy, { clip.setText(AnnotatedString(Prefs.code)); copied = true }, Modifier.weight(1f), if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy)
                GhostButton(s.newCode, {
                    Prefs.newCode()
                    // the running server still has the old password: restart it so QR, code and server agree
                    if (running) { actions.startHost(); Toast.makeText(ctx, s.codeRegeneratedRestart, Toast.LENGTH_LONG).show() }
                    else Toast.makeText(ctx, s.codeRegenerated, Toast.LENGTH_SHORT).show()
                }, Modifier.weight(1f), Icons.Rounded.Refresh)
            }
        }
    }
}

/* ───────────── Tab 3: devices + traffic ───────────── */

@Composable
private fun DevicesTab(running: Boolean) {
    val s = LocalStrings.current
    val hs = LocalHs.current
    val info by HostService.state.collectAsState()

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(Icons.Rounded.People, s.devices, s.n(info.clients), Modifier.weight(1f))
        StatTile(Icons.Rounded.ArrowUpward, s.upload, formatBytes(info.up), Modifier.weight(1f))
        StatTile(Icons.Rounded.ArrowDownward, s.download, formatBytes(info.down), Modifier.weight(1f))
    }

    if (!running || info.devices.isEmpty()) {
        GlassCard(Modifier.fillMaxWidth()) {
            Text(
                if (running) s.waitingDevices else s.qrAfterStart, color = hs.sub, fontSize = 13.sp, lineHeight = 21.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(20.dp)
            )
        }
    } else {
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                val now = System.currentTimeMillis()
                info.devices.forEachIndexed { i, d ->
                    if (i > 0) Divider()
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(Modifier.size(42.dp).clip(CircleShape).background(hs.brand), contentAlignment = Alignment.Center) {
                            Text(d.name.trim().take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(d.name, color = hs.text, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                            Text("${d.ip} · ${s.connectedFor(((now - d.since) / 60_000).toInt())}", color = hs.sub, fontSize = 12.sp, maxLines = 1)
                        }
                        Box(Modifier.size(10.dp).clip(CircleShape).background(Mint))
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckItem(
    icon: ImageVector, title: String, value: String, level: Level,
    actionLabel: String? = null, onAction: (() -> Unit)? = null, note: String? = null
) {
    val hs = LocalHs.current
    val target = when (level) { Level.OK -> Mint; Level.WARN -> Amber; Level.INFO -> Cyan }
    val c by animateColorAsState(target, tween(400), label = "ck")
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBubble(icon, c, 42.dp)
            Column(Modifier.weight(1f)) {
                Text(title, color = hs.sub, fontSize = 12.sp)
                Text(value, color = hs.text, fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 21.sp)
            }
            if (level == Level.WARN && actionLabel != null && onAction != null) {
                Box(
                    Modifier.widthIn(min = 76.dp, max = 130.dp).pressable(onClick = onAction).clip(CircleShape).background(hs.brand).padding(horizontal = 14.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) { Text(actionLabel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 2, textAlign = TextAlign.Center) }
            } else {
                Icon(
                    when (level) { Level.OK -> Icons.Rounded.CheckCircle; Level.WARN -> Icons.Rounded.ErrorOutline; Level.INFO -> Icons.Rounded.Info },
                    null, tint = c, modifier = Modifier.size(24.dp)
                )
            }
        }
        if (note != null) Text(note, color = hs.sub, fontSize = 12.sp, lineHeight = 19.sp, modifier = Modifier.padding(start = 56.dp))
    }
}

@Composable
private fun StatTile(icon: ImageVector, label: String, value: String, modifier: Modifier) {
    val hs = LocalHs.current
    GlassCard(modifier, radius = 20.dp) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = Cyan, modifier = Modifier.size(18.dp))
            Text(value, color = hs.text, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
            Text(label, color = hs.sub, fontSize = 12.sp)
        }
    }
}
