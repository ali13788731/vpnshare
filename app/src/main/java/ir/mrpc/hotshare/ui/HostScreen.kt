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

    Column(Modifier.fillMaxSize()) {
        TopBar(s.hostTitle, onBack = nav.back)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // hero orb
            Box(Modifier.fillMaxWidth().height(190.dp).appear(0), contentAlignment = Alignment.Center) {
                if (info.running) PulseRings(Modifier.size(190.dp), Mint) else PulseRings(Modifier.size(160.dp), Violet, rings = 2, periodMs = 5200)
                OrbitRings(Modifier.size(150.dp), speed = if (info.running) 1.6f else 0.6f)
                BrandMark(78.dp)
            }
            if (info.running) {
                Text(s.sharing, color = Mint, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }

            // checklist
            GlassCard(Modifier.fillMaxWidth().appear(1)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CheckItem(
                        Icons.Rounded.WifiTethering, s.stepHotspot,
                        when {
                            info.ap == ApMode.LOCAL -> s.hotspotAuto
                            hotspotOk -> s.hotspotSystem
                            info.running && !info.apFailed && Prefs.autoHotspot -> s.hotspotWaiting
                            else -> s.hotspotOff
                        }, hotspotOk
                    )
                    CheckItem(Icons.Rounded.VpnKey, s.stepVpn, if (vpnOk) s.vpnOn else s.vpnOff, vpnOk)
                    CheckItem(Icons.Rounded.Visibility, s.stepVisible, if (visibleOk) s.visibleOn else s.visibleOff, visibleOk)
                }
            }

            if (!hotspotOk && (!Prefs.autoHotspot || info.apFailed || !info.running)) {
                GlassCard(Modifier.fillMaxWidth().appear(2)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(s.hotspotNote, color = hs.sub, fontSize = 13.sp, lineHeight = 20.sp)
                        GhostButton(s.openHotspot, { Net.openHotspotSettings(ctx) }, Modifier.fillMaxWidth(), Icons.Rounded.WifiTethering)
                    }
                }
            }
            if (!vpnOk) {
                Text(s.vpnNote, color = Amber, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(horizontal = 6.dp))
            }

            if (info.running) {
                val offer = remember(info.ip, info.ssid, info.psk, Prefs.code, Prefs.apSsid, Prefs.apPsk) { HostService.currentOffer(ctx) }
                val uri = remember(offer) { Pairing.toUri(offer) }

                // QR
                GlassCard(Modifier.fillMaxWidth().appear(3)) {
                    Column(Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(Modifier.clip(RoundedCornerShape(28.dp)).background(Color.White).padding(14.dp)) {
                            QrCodeView(uri, Modifier.size(240.dp)) {
                                Icon(Icons.Rounded.Shield, null, tint = Violet, modifier = Modifier.size(26.dp))
                            }
                        }
                        Text(s.scanHint, color = hs.sub, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                }

                // Code
                GlassCard(Modifier.fillMaxWidth().appear(4)) {
                    val clip = LocalClipboardManager.current
                    var copied by remember { mutableStateOf(false) }
                    LaunchedEffect(copied) { if (copied) { delay(1500); copied = false } }
                    Column(Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(s.codeLabel, color = hs.sub, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        DigitBoxes(Prefs.code, boxWidth = 30.dp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            GhostButton(if (copied) s.copied else s.copy, { clip.setText(AnnotatedString(Prefs.code)); copied = true }, Modifier.weight(1f), if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy)
                            GhostButton(s.newCode, { Prefs.newCode(); actions.startHost() }, Modifier.weight(1f), Icons.Rounded.Refresh)
                        }
                    }
                }

                // Stats
                Row(Modifier.fillMaxWidth().appear(5), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(Icons.Rounded.People, s.devices, info.clients.toString(), Modifier.weight(1f))
                    StatTile(Icons.Rounded.ArrowUpward, s.upload, formatBytes(info.up), Modifier.weight(1f))
                    StatTile(Icons.Rounded.ArrowDownward, s.download, formatBytes(info.down), Modifier.weight(1f))
                }
                GhostButton(s.stopShare, { actions.stopHost() }, Modifier.fillMaxWidth(), Icons.Rounded.PowerSettingsNew, Rose)
            } else {
                GradientButton(s.startShare, { actions.startHost() }, Modifier.appear(3), Icons.Rounded.PlayArrow)
            }
        }
    }
}

@Composable
private fun CheckItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String, ok: Boolean) {
    val hs = LocalHs.current
    val c by animateColorAsState(if (ok) Mint else Amber, tween(400), label = "ck")
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        IconBubble(icon, c, 42.dp)
        Column(Modifier.weight(1f)) {
            Text(title, color = hs.sub, fontSize = 12.sp)
            Text(value, color = hs.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Icon(if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline, null, tint = c, modifier = Modifier.size(24.dp))
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
