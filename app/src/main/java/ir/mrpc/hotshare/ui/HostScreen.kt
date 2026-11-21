package ir.mrpc.hotshare.ui

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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.mrpc.hotshare.core.Net
import ir.mrpc.hotshare.core.Pairing
import ir.mrpc.hotshare.core.Prefs
import ir.mrpc.hotshare.host.ApMode
import ir.mrpc.hotshare.host.HostService
import kotlinx.coroutines.delay

private class PreStatus(val hotspot: Boolean, val vpn: Boolean, val bt: Boolean)

@Composable
fun HostScreen(nav: Nav, actions: Actions) {
    val s = LocalStrings.current
    val hs = LocalHs.current
    val ctx = LocalContext.current
    val info by HostService.state.collectAsState()
    val pre by produceState(PreStatus(false, false, false)) {
        while (true) {
            value = PreStatus(Net.hotspotIp() != null, Net.isVpnActive(ctx), Net.bluetoothOn(ctx))
            delay(1500)
        }
    }
    LaunchedEffect(Unit) {
        delay(600)
        if (Prefs.autoStart && !HostService.state.value.running && Net.hotspotIp() != null) actions.startHost()
    }

    val hotspotOk = info.ap != ApMode.NONE || (!info.running && pre.hotspot)
    val vpnOk = if (info.running) info.vpn else pre.vpn
    val visibleOk = if (info.running) info.ble else pre.bt

    var showAp by remember { mutableStateOf(false) }
    if (showAp) HotspotDetailsSheet(Prefs.apSsid, Prefs.apPsk, { a, b -> Prefs.apSsid = a; Prefs.apPsk = b; Prefs.save(); showAp = false }, { showAp = false })

    Column(Modifier.fillMaxSize()) {
        TopBar(s.hostTitle, onBack = nav.back)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // hero orb
            Box(Modifier.fillMaxWidth().height(if (info.running) 150.dp else 170.dp).appear(0), contentAlignment = Alignment.Center) {
                if (info.running) PulseRings(Modifier.size(150.dp), Mint) else PulseRings(Modifier.size(140.dp), Violet, rings = 2, periodMs = 5200)
                OrbitRings(Modifier.size(120.dp), speed = if (info.running) 1.6f else 0.6f)
                BrandMark(64.dp)
            }
            if (info.running) {
                Text(s.sharing, color = Mint, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }

            // checklist: every orange item has its own fix button
            GlassCard(Modifier.fillMaxWidth().appear(1)) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    CheckItem(
                        Icons.Rounded.WifiTethering, s.stepHotspot,
                        when {
                            info.ap == ApMode.LOCAL -> s.hotspotAuto
                            hotspotOk -> s.hotspotSystem
                            info.running && !info.apFailed && Prefs.autoHotspot -> s.hotspotWaiting
                            else -> s.hotspotOff
                        }, hotspotOk, s.openHotspot, { Net.openHotspotSettings(ctx) }
                    )
                    CheckItem(Icons.Rounded.VpnKey, s.stepVpn, if (vpnOk) s.vpnOn else s.vpnOff, vpnOk, s.fixOpen, { Net.openVpnSettings(ctx) })
                    CheckItem(Icons.Rounded.Visibility, s.stepVisible, if (visibleOk) s.visibleOn else s.visibleOff, visibleOk, s.fixTurnOn, { actions.enableBluetooth() })
                }
            }

            // one short hint at a time
            when {
                !vpnOk -> Text(s.vpnNote, color = Amber, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(horizontal = 6.dp))
                !hotspotOk && (!Prefs.autoHotspot || info.apFailed) ->
                    Text(s.hotspotNote, color = hs.sub, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(horizontal = 6.dp))
            }

            if (info.running) {
                val offer = remember(info.ip, info.ssid, info.psk, Prefs.code, Prefs.apSsid, Prefs.apPsk) { HostService.currentOffer(ctx) }
                val uri = remember(offer) { Pairing.toUri(offer) }
                val hotspotName = offer.ssid

                // hotspot name: the receiver's phone must join exactly this network
                GlassCard(Modifier.fillMaxWidth().appear(2)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IconBubble(Icons.Rounded.Wifi, if (hotspotName != null) Mint else Amber, 40.dp)
                            Column(Modifier.weight(1f)) {
                                Text(s.hostHotspotName, color = hs.sub, fontSize = 12.sp)
                                Text(hotspotName ?: s.hostHotspotNotSet, color = hs.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            if (info.ap != ApMode.LOCAL) {
                                Box(
                                    Modifier.pressable { showAp = true }.clip(CircleShape).background(hs.brand).padding(horizontal = 16.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) { Text(s.hostHotspotEdit, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                            }
                        }
                        if (hotspotName == null) Text(s.hostHotspotHint, color = hs.sub, fontSize = 12.sp, lineHeight = 18.sp)
                    }
                }

                // QR + code in one card
                GlassCard(Modifier.fillMaxWidth().appear(3)) {
                    val clip = LocalClipboardManager.current
                    var copied by remember { mutableStateOf(false) }
                    LaunchedEffect(copied) { if (copied) { delay(1500); copied = false } }
                    Column(Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(Modifier.clip(RoundedCornerShape(28.dp)).background(Color.White).padding(14.dp)) {
                            QrCodeView(uri, Modifier.size(220.dp)) {
                                Icon(Icons.Rounded.Shield, null, tint = Violet, modifier = Modifier.size(26.dp))
                            }
                        }
                        Text(s.scanHint, color = hs.sub, fontSize = 13.sp, textAlign = TextAlign.Center)
                        DigitBoxes(Prefs.code, boxWidth = 30.dp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            GhostButton(if (copied) s.copied else s.copy, { clip.setText(AnnotatedString(Prefs.code)); copied = true }, Modifier.weight(1f), if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy)
                            GhostButton(s.newCode, { Prefs.newCode(); actions.startHost() }, Modifier.weight(1f), Icons.Rounded.Refresh)
                        }
                    }
                }

                // Stats
                Row(Modifier.fillMaxWidth().appear(4), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(Icons.Rounded.People, s.devices, info.clients.toString(), Modifier.weight(1f))
                    StatTile(Icons.Rounded.ArrowUpward, s.upload, formatBytes(info.up), Modifier.weight(1f))
                    StatTile(Icons.Rounded.ArrowDownward, s.download, formatBytes(info.down), Modifier.weight(1f))
                }
                GhostButton(s.stopShare, { actions.stopHost() }, Modifier.fillMaxWidth(), Icons.Rounded.PowerSettingsNew, Rose)
            } else {
                GradientButton(s.startShare, { actions.startHost() }, Modifier.appear(2), Icons.Rounded.PlayArrow)
            }
        }
    }
}

@Composable
private fun CheckItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String, ok: Boolean,
    actionLabel: String? = null, onAction: (() -> Unit)? = null
) {
    val hs = LocalHs.current
    val c by animateColorAsState(if (ok) Mint else Amber, tween(400), label = "ck")
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        IconBubble(icon, c, 42.dp)
        Column(Modifier.weight(1f)) {
            Text(title, color = hs.sub, fontSize = 12.sp)
            Text(value, color = hs.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        if (!ok && actionLabel != null && onAction != null) {
            Box(
                Modifier.widthIn(max = 130.dp).pressable(onClick = onAction).clip(CircleShape).background(hs.brand).padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) { Text(actionLabel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 2, textAlign = TextAlign.Center) }
        } else {
            Icon(if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline, null, tint = c, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun StatTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, modifier: Modifier) {
    val hs = LocalHs.current
    GlassCard(modifier, radius = 20.dp) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = Cyan, modifier = Modifier.size(18.dp))
            Text(value, color = hs.text, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
            Text(label, color = hs.sub, fontSize = 12.sp)
        }
    }
}
