package ir.mrpc.hotshare.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.mrpc.hotshare.core.Net
import ir.mrpc.hotshare.core.Pairing
import ir.mrpc.hotshare.core.Prefs
import ir.mrpc.hotshare.host.HostService
import kotlinx.coroutines.delay

@Composable
fun HostScreen(actions: Actions, nav: (Screen) -> Unit, onBack: () -> Unit) {
    val s = LocalStrings.current
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val info by HostService.state.collectAsState()
    var ip by remember { mutableStateOf<String?>(null) }
    var hotspotOn by remember { mutableStateOf(false) }
    var vpnOn by remember { mutableStateOf(false) }
    var userStopped by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            ip = Net.hotspotIp()
            hotspotOn = ip != null || Net.hotspotFlag(ctx)
            vpnOn = Net.isVpnActive(ctx)
            delay(1500)
        }
    }
    // شروع/توقف خودکار با روشن/خاموش شدن هات‌اسپات
    LaunchedEffect(hotspotOn, info.running, userStopped) {
        if (!hotspotOn) {
            if (userStopped) userStopped = false
            if (info.running) actions.stopHost()
        } else if (Prefs.autoStart && !info.running && !userStopped) {
            actions.startHost()
        }
    }

    AuroraBackground {
        Column(Modifier.fillMaxSize()) {
            TopBar(s.shareTitle, onBack, nav)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                StepCard(1, s.stepHotspot, if (hotspotOn) s.hotspotOn else s.hotspotOff, if (hotspotOn) Good else Warn) {
                    if (!hotspotOn) {
                        Spacer(Modifier.height(10.dp))
                        Text(s.hotspotNote, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f))
                        Spacer(Modifier.height(12.dp))
                        GradientButton(s.openHotspot, Modifier.fillMaxWidth(), Icons.Rounded.WifiTethering) { Net.openHotspotSettings(ctx) }
                    }
                }
                StepCard(2, s.stepVpn, if (vpnOn) s.vpnOn else s.vpnOff, if (vpnOn) Good else Warn) {
                    if (!vpnOn) {
                        Spacer(Modifier.height(10.dp))
                        Text(s.vpnNote, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f))
                    }
                }

                if (!info.running) {
                    GradientButton(s.startShare, Modifier.fillMaxWidth(), Icons.Rounded.Bolt, enabled = hotspotOn) {
                        userStopped = false; actions.startHost()
                    }
                }

                AnimatedVisibility(visible = info.running) {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            PulseRings(true, Modifier.size(44.dp), Good) {
                                Icon(Icons.Rounded.WifiTethering, null, tint = Good)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(s.sharing, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(s.scanHint, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        val link = Pairing.toUri(ip ?: "192.168.43.1", Prefs.code)
                        val qr = remember(link) { qrBitmap(link) }
                        Image(
                            qr, null,
                            Modifier.align(Alignment.CenterHorizontally).size(230.dp)
                                .clip(RoundedCornerShape(20.dp)).background(Color.White).padding(10.dp)
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(s.codeLabel, Modifier.align(Alignment.CenterHorizontally), fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
                        Text(
                            Pairing.pretty(Prefs.code), Modifier.align(Alignment.CenterHorizontally),
                            style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp, textDirection = TextDirection.Ltr),
                            color = Cyan
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton({
                                clipboard.setText(AnnotatedString(Prefs.code))
                                Toast.makeText(ctx, s.copied, Toast.LENGTH_SHORT).show()
                            }, Modifier.weight(1f)) {
                                Icon(Icons.Rounded.ContentCopy, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(s.copy)
                            }
                            OutlinedButton({ Prefs.newCode(); actions.startHost() }, Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Refresh, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(s.newCode)
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatBox(Icons.Rounded.Devices, s.devices, "${info.clients}", Modifier.weight(1f))
                            StatBox(Icons.Rounded.ArrowUpward, s.upload, fmtBytes(info.up), Modifier.weight(1f))
                            StatBox(Icons.Rounded.ArrowDownward, s.download, fmtBytes(info.down), Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton({ userStopped = true; actions.stopHost() }, Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Bad)) {
                            Icon(Icons.Rounded.Stop, null); Spacer(Modifier.width(8.dp)); Text(s.stopShare, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
