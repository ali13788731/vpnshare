package ir.mrpc.hotshare.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.mrpc.hotshare.client.*
import ir.mrpc.hotshare.core.Strings
import kotlinx.coroutines.delay

private fun errorText(s: Strings, e: ConnError) = when (e) {
    ConnError.WRONG_CODE -> s.errCode
    ConnError.UNREACHABLE -> s.errUnreach
    ConnError.ENGINE -> s.errNative
    ConnError.VPN_DENIED -> s.errVpn
    ConnError.LOST -> s.errLost
    ConnError.BAD_QR -> s.errQr
    ConnError.DENIED -> s.errDenied
    ConnError.BLE -> s.errBle
    ConnError.WIFI -> s.errWifi
    ConnError.CANCELED -> s.errCanceled
}

@Composable
fun ConnectScreen(nav: Nav) {
    val s = LocalStrings.current
    val flow by Connector.flow.collectAsState()

    // Wi-Fi password sheet, asked inside the app
    (flow as? ConnFlow.NeedWifi)?.let { nw ->
        WifiSheet(nw.ssid, nw.retry, onSubmit = { a, b -> Connector.submitWifi(a, b) }, onDismiss = { Connector.cancelWifi() })
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(s.appName, onBack = {
            when (flow) {
                is ConnFlow.Working, is ConnFlow.NeedWifi -> Connector.cancel()
                is ConnFlow.Failed -> Connector.reset()
                else -> {}
            }
            nav.back()
        })
        AnimatedContent(
            targetState = when (flow) {
                is ConnFlow.Done -> 1
                is ConnFlow.Failed -> 2
                else -> 0
            },
            transitionSpec = { (fadeIn(tween(400)) + scaleIn(initialScale = 0.9f, animationSpec = tween(400))) togetherWith fadeOut(tween(200)) },
            modifier = Modifier.weight(1f).fillMaxWidth(), label = "connect"
        ) { page ->
            when (page) {
                1 -> DonePage(nav)
                2 -> FailedPage(nav, (flow as? ConnFlow.Failed)?.error ?: ConnError.UNREACHABLE)
                else -> WorkingPage(flow)
            }
        }
    }
}

@Composable
private fun WorkingPage(flow: ConnFlow) {
    val s = LocalStrings.current
    val hs = LocalHs.current
    val (stage, host) = when (flow) {
        is ConnFlow.Working -> flow.stage to flow.host
        is ConnFlow.NeedWifi -> Stage.WIFI to flow.host
        else -> Stage.WIFI to null
    }
    val via = Connector.viaDevice
    val stages = buildList {
        if (via) add(Stage.REQUEST to s.stageRequest)
        add(Stage.WIFI to s.stageWifi)
        add(Stage.VERIFY to s.stageVerify)
        add(Stage.TUNNEL to s.stageTunnel)
    }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterVertically)
    ) {
        Box(Modifier.size(230.dp), contentAlignment = Alignment.Center) {
            PulseRings(Modifier.fillMaxSize(), Cyan)
            OrbitRings(Modifier.size(190.dp), speed = 1.4f)
            BrandMark(84.dp)
        }
        Text(s.connectingTo, color = hs.sub, fontSize = 14.sp)
        Text(host ?: "HotShare", color = hs.text, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                val cur = stages.indexOfFirst { it.first == stage }.coerceAtLeast(0)
                stages.forEachIndexed { i, (_, label) ->
                    StageRow(label, when { i < cur -> StageState.DONE; i == cur -> StageState.ACTIVE; else -> StageState.PENDING })
                }
            }
        }
        GhostButton(LocalStrings.current.cancel, { Connector.cancel() }, Modifier.fillMaxWidth())
    }
}

@Composable
private fun DonePage(nav: Nav) {
    val s = LocalStrings.current
    val hs = LocalHs.current
    val state by ClientVpnService.state.collectAsState()
    val up by ClientVpnService.up.collectAsState()
    val down by ClientVpnService.down.collectAsState()
    val host = (Connector.flow.value as? ConnFlow.Done)?.host
    val since = (state as? ClientState.Connected)?.since ?: System.currentTimeMillis()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }

    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(0.45f, 220f)) }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically)
    ) {
        Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
            PulseRings(Modifier.fillMaxSize(), Mint, periodMs = 3000)
            Box(
                Modifier.size(110.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }.clip(CircleShape)
                    .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Mint, Cyan))),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(64.dp)) }
        }
        Text(s.connected, color = hs.text, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        if (host != null) Text(s.routedVia(host), color = hs.sub, fontSize = 14.sp, textAlign = TextAlign.Center)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DoneStat(Icons.Rounded.Timer, s.duration, formatDuration(now - since), Modifier.weight(1.3f))
            DoneStat(Icons.Rounded.ArrowUpward, s.upload, formatBytes(up), Modifier.weight(1f))
            DoneStat(Icons.Rounded.ArrowDownward, s.download, formatBytes(down), Modifier.weight(1f))
        }
        GhostButton(s.disconnect, { Connector.disconnect(); nav.back() }, Modifier.fillMaxWidth(), Icons.Rounded.PowerSettingsNew, Rose)
    }
}

@Composable
private fun DoneStat(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, modifier: Modifier) {
    val hs = LocalHs.current
    GlassCard(modifier, radius = 20.dp) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = Mint, modifier = Modifier.size(18.dp))
            Text(value, color = hs.text, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
            Text(label, color = hs.sub, fontSize = 12.sp)
        }
    }
}

@Composable
private fun FailedPage(nav: Nav, error: ConnError) {
    val s = LocalStrings.current
    val hs = LocalHs.current
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically)
    ) {
        Box(Modifier.size(110.dp).clip(CircleShape).background(Rose.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.ErrorOutline, null, tint = Rose, modifier = Modifier.size(60.dp))
        }
        Text(s.failedTitle, color = hs.text, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text(errorText(s, error), color = hs.sub, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 22.sp)
        Spacer(Modifier.height(8.dp))
        GradientButton(s.retry, { Connector.reset(); nav.back() }, icon = Icons.Rounded.Refresh)
        GhostButton(s.back, { Connector.reset(); nav.tab(Screen.HOME) }, Modifier.fillMaxWidth())
    }
}
