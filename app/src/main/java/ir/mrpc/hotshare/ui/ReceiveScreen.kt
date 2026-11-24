package ir.mrpc.hotshare.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.mrpc.hotshare.ble.BleScanner
import ir.mrpc.hotshare.ble.NearbyDevice
import ir.mrpc.hotshare.core.Net
import ir.mrpc.hotshare.core.Perms
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun ReceiveScreen(nav: Nav, actions: Actions, onCode: () -> Unit) {
    val s = LocalStrings.current
    val hs = LocalHs.current
    val ctx = LocalContext.current
    val myName = remember { Net.deviceName(ctx) }

    var hasPerm by remember { mutableStateOf(Perms.has(ctx, Perms.bleClient())) }
    val btOn by produceState(Net.bluetoothOn(ctx)) { while (true) { value = Net.bluetoothOn(ctx); delay(1200) } }
    val wifiOn by produceState(Net.wifiEnabled(ctx)) { while (true) { value = Net.wifiEnabled(ctx); delay(1500) } }
    val scanner = remember { BleScanner(ctx.applicationContext) }
    val devices by scanner.devices.collectAsState()
    var longWait by remember { mutableStateOf(false) }

    // the app asks by itself: permission first, then Android's own "turn on Bluetooth?" prompt
    LaunchedEffect(Unit) {
        delay(400)
        if (!hasPerm) {
            actions.requestPerms(Perms.bleClient()) { ok ->
                hasPerm = ok
                if (ok && !Net.bluetoothOn(ctx)) actions.enableBluetooth()
            }
        } else if (!Net.bluetoothOn(ctx)) actions.enableBluetooth()
    }

    LaunchedEffect(hasPerm, btOn) {
        if (hasPerm && btOn) {
            scanner.start()
            longWait = false
            delay(9000)
            longWait = true
        } else scanner.stop()
    }
    LaunchedEffect(Unit) { while (true) { delay(1000); scanner.prune() } }
    DisposableEffect(Unit) { onDispose { scanner.stop() } }

    val slots = remember { SnapshotStateMap<String, Int>() }
    var nextSlot by remember { mutableIntStateOf(0) }
    devices.forEach { if (slots[it.address] == null) { slots[it.address] = nextSlot; nextSlot++ } }
    val nearest = devices.firstOrNull()

    Column(Modifier.fillMaxSize()) {
        TopBar(s.receiveHeader, onBack = nav.back)

        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().appear(0)) {
            val boxW = maxWidth; val boxH = maxHeight
            val radarSize = minOf(boxW, boxH) - 12.dp
            val density = androidx.compose.ui.platform.LocalDensity.current
            val maxR = with(density) { (radarSize / 2).toPx() }

            // radar
            Box(Modifier.align(Alignment.Center).size(radarSize), contentAlignment = Alignment.Center) {
                RadarCanvas(Modifier.fillMaxSize(), scanning = hasPerm && btOn)
                OrbitRings(Modifier.size(radarSize * 0.5f), speed = 1.2f)
                // my name, big
                Box(
                    Modifier.size(radarSize * 0.38f).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Cyan.copy(alpha = 0.28f), Violet.copy(alpha = 0.28f))))
                        .border(2.dp, hs.brand, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        myName, color = hs.text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center,
                        maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // nearby devices
            devices.take(6).forEach { d ->
                val slot = slots[d.address] ?: 0
                val isNearest = d.address == nearest?.address
                key(d.address) {
                    val frac by animateFloatAsState(((-d.rssi - 40f) / 55f).coerceIn(0f, 1f), tween(800), label = "frac")
                    val r = maxR * (0.62f + 0.3f * frac)
                    val ang = Math.toRadians((slot * 137.5 + 25.0))
                    val cx = with(density) { (boxW / 2).toPx() } + (cos(ang) * r).toFloat()
                    val cy = with(density) { (boxH / 2).toPx() } + (sin(ang) * r).toFloat()
                    val half = with(density) { 48.dp.toPx() }
                    Box(
                        Modifier.size(96.dp).offset { IntOffset((cx - half).roundToInt(), (cy - half).roundToInt()) }.appear(0),
                        contentAlignment = Alignment.Center
                    ) {
                        DeviceBubble(d, isNearest) { actions.connectDevice(d, scanner.handle(d.address)) }
                    }
                }
            }
        }

        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                !hasPerm -> InfoCard(s.permNearby, s.permGrant) { actions.requestPerms(Perms.bleClient()) { hasPerm = it } }
                !btOn -> InfoCard(s.btOffNote, s.btTurnOn) { actions.enableBluetooth() }
                !wifiOn -> InfoCard(s.wifiOffNote, s.turnOnWifi, Icons.Rounded.Wifi) { Net.openWifiPanel(ctx) }
                nearest != null -> {
                    AnimatedVisibility(true, enter = fadeIn() + slideInVertically { it / 2 }) {
                        GlassCard(Modifier.fillMaxWidth(), onClick = { actions.connectDevice(nearest, scanner.handle(nearest.address)) }) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Avatar(nearest.name, 52.dp, true)
                                Column(Modifier.weight(1f)) {
                                    Text(s.nearest, color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(nearest.name, color = hs.text, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(s.tapToConnect, color = hs.sub, fontSize = 12.sp)
                                }
                                Box(Modifier.clip(CircleShape).background(hs.brand).padding(horizontal = 18.dp, vertical = 10.dp)) {
                                    Text(s.connectNow, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
                else -> Text(
                    if (longWait) s.nothingFound else s.searching,
                    color = if (longWait) Amber else hs.sub, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton(s.scanQr, { nav.go(Screen.SCAN) }, Modifier.weight(1f), Icons.Rounded.QrCodeScanner, if (longWait && nearest == null) Cyan else hs.text)
                GhostButton(s.enterCode, onCode, Modifier.weight(1f), Icons.Rounded.Keyboard, if (longWait && nearest == null) Cyan else hs.text)
            }
        }
    }
}

@Composable
private fun InfoCard(text: String, action: String, icon: ImageVector = Icons.Rounded.Bluetooth, onClick: () -> Unit) {
    val hs = LocalHs.current
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBubble(icon, Cyan, 40.dp)
                Text(text, color = hs.text, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f))
            }
            GradientButton(action, onClick)
        }
    }
}

@Composable
private fun Avatar(name: String, size: androidx.compose.ui.unit.Dp, glow: Boolean) {
    val hs = LocalHs.current
    Box(
        Modifier.size(size).clip(CircleShape).background(if (glow) hs.brand else Brush.linearGradient(listOf(Violet, Cyan))),
        contentAlignment = Alignment.Center
    ) {
        Text(name.trim().take(1).uppercase(), color = Color.White, fontSize = (size.value * 0.42f).sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun DeviceBubble(d: NearbyDevice, nearest: Boolean, onClick: () -> Unit) {
    val hs = LocalHs.current
    val t = rememberInfiniteTransition(label = "float")
    val f by t.animateFloat(-4f, 4f, infiniteRepeatable(tween(2200 + (d.address.hashCode() and 511), easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "f")
    Column(
        Modifier.graphicsLayer { translationY = f }.pressable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (nearest) PulseRings(Modifier.size(92.dp), Mint, rings = 2, periodMs = 2200)
            Avatar(d.name, if (nearest) 62.dp else 50.dp, nearest)
        }
        Text(d.name, color = hs.text, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 90.dp))
    }
}

@Composable
private fun RadarCanvas(modifier: Modifier, scanning: Boolean) {
    val t = rememberInfiniteTransition(label = "radar")
    val sweep by t.animateFloat(0f, 360f, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "sweep")
    val rings by t.animateFloat(0f, 1f, infiniteRepeatable(tween(3800, easing = LinearEasing)), label = "rings")
    val dash by t.animateFloat(0f, 360f, infiniteRepeatable(tween(26000, easing = LinearEasing)), label = "dash")
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height / 2)
        val R = size.minDimension / 2f
        // dashed orbits
        listOf(0.55f, 0.78f, 0.98f).forEachIndexed { i, f ->
            rotate(if (i % 2 == 0) dash else -dash, c) {
                drawCircle(Color.White.copy(alpha = 0.16f), R * f, c, style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 16f))))
            }
        }
        if (scanning) {
            // expanding ripples
            repeat(3) { i ->
                val q = (rings + i / 3f) % 1f
                drawCircle(Cyan.copy(alpha = (1f - q) * 0.30f), R * (0.2f + 0.8f * q), c, style = Stroke(3f))
            }
            // sweep
            rotate(sweep, c) {
                drawArc(
                    Brush.sweepGradient(listOf(Color.Transparent, Color.Transparent, Cyan.copy(alpha = 0.0f), Cyan.copy(alpha = 0.35f)), c),
                    -70f, 70f, true, Offset(c.x - R, c.y - R), Size(R * 2, R * 2)
                )
            }
        }
    }
}
