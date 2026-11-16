package ir.mrpc.hotshare.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.mrpc.hotshare.client.ClientError
import ir.mrpc.hotshare.client.ClientState
import ir.mrpc.hotshare.client.ClientVpnService
import ir.mrpc.hotshare.core.Net
import ir.mrpc.hotshare.core.Pairing
import ir.mrpc.hotshare.core.Strings
import kotlinx.coroutines.delay

private fun errText(s: Strings, e: ClientError) = when (e) {
    ClientError.WRONG_CODE -> s.errCode
    ClientError.UNREACHABLE -> s.errUnreach
    ClientError.NATIVE -> s.errNative
    ClientError.VPN_DENIED -> s.errVpn
    ClientError.LOST -> s.errLost
    ClientError.BAD_QR -> s.errQr
    ClientError.NO_WIFI -> s.errNoWifi
}

@Composable
fun ClientScreen(actions: Actions, nav: (Screen) -> Unit, onBack: () -> Unit) {
    val s = LocalStrings.current
    val ctx = LocalContext.current
    val st by ClientVpnService.state.collectAsState()
    val stats by ClientVpnService.stats.collectAsState()
    var wifi by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var localErr by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (true) { wifi = Net.gatewayIp(ctx) != null; delay(1500) }
    }

    AuroraBackground {
        Column(Modifier.fillMaxSize()) {
            TopBar(s.connectTitle, onBack, nav)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (val c = st) {
                    is ClientState.Connected -> {
                        val elapsed by produceState(0L, c.since) {
                            while (true) { value = (System.currentTimeMillis() - c.since) / 1000; delay(1000) }
                        }
                        GlassCard(Modifier.fillMaxWidth()) {
                            PulseRings(true, Modifier.align(Alignment.CenterHorizontally).size(190.dp), Good) {
                                Box(Modifier.size(96.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Good, Cyan))), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.CheckCircle, null, tint = Color.White, modifier = Modifier.size(52.dp))
                                }
                            }
                            Text(s.connected, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.height(16.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                StatBox(Icons.Rounded.Bolt, s.duration, fmtTime(elapsed), Modifier.weight(1f))
                                StatBox(Icons.Rounded.ArrowUpward, s.upload, fmtBytes(stats.first), Modifier.weight(1f))
                                StatBox(Icons.Rounded.ArrowDownward, s.download, fmtBytes(stats.second), Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(18.dp))
                            OutlinedButton({ actions.disconnect() }, Modifier.fillMaxWidth().height(52.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Bad)) {
                                Icon(Icons.Rounded.PowerSettingsNew, null); Spacer(Modifier.width(8.dp)); Text(s.disconnect, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    is ClientState.Connecting -> {
                        GlassCard(Modifier.fillMaxWidth()) {
                            PulseRings(true, Modifier.align(Alignment.CenterHorizontally).size(190.dp), Cyan) {
                                CircularProgressIndicator(color = Cyan)
                            }
                            Text(s.connecting, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    else -> {
                        val err = (c as? ClientState.Error)?.let { errText(s, it.kind) } ?: localErr
                        AnimatedVisibility(visible = err != null) {
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Bad.copy(alpha = .15f)).padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.ErrorOutline, null, tint = Bad)
                                Spacer(Modifier.width(10.dp))
                                Text(err ?: "", color = Bad, fontSize = 14.sp)
                            }
                        }

                        StepCard(1, s.stepWifi, if (wifi) s.wifiOk else s.wifiNo, if (wifi) Good else Warn) {
                            if (!wifi) {
                                Spacer(Modifier.height(10.dp))
                                Text(s.wifiNote, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f))
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton({ Net.openWifiSettings(ctx) }, Modifier.fillMaxWidth()) {
                                    Icon(Icons.Rounded.Wifi, null); Spacer(Modifier.width(8.dp)); Text(s.openWifi)
                                }
                            }
                        }

                        GlassCard(Modifier.fillMaxWidth()) {
                            PulseRings(true, Modifier.align(Alignment.CenterHorizontally).size(210.dp)) {
                                Box(
                                    Modifier.size(120.dp).clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(Cyan, Violet)))
                                        .clickable { localErr = null; actions.scan() },
                                    contentAlignment = Alignment.Center
                                ) { Icon(Icons.Rounded.QrCodeScanner, null, tint = Color.White, modifier = Modifier.size(58.dp)) }
                            }
                            Text(s.scanQr, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }

                        Text(s.orEnter, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = .6f))

                        GlassCard(Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = code,
                                onValueChange = { code = Pairing.normalizeDigits(it).filter(Char::isDigit).take(8) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text(s.enterCode) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = TextStyle(fontSize = 24.sp, letterSpacing = 6.sp, textAlign = TextAlign.Center, textDirection = TextDirection.Ltr),
                                shape = RoundedCornerShape(16.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            GradientButton(s.connect, Modifier.fillMaxWidth(), Icons.Rounded.VpnKey, enabled = code.length == 8) {
                                val gw = Net.gatewayIp(ctx)
                                when {
                                    code.length != 8 -> localErr = s.errBadCode
                                    gw == null -> localErr = s.errNoWifi
                                    else -> { localErr = null; actions.connect(Pairing.Link(gw, code)) }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
