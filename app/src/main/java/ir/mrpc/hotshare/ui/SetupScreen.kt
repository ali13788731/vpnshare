package ir.mrpc.hotshare.ui

import android.Manifest
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.mrpc.hotshare.core.Net
import ir.mrpc.hotshare.core.Perms
import kotlinx.coroutines.delay

private class Snap(
    val nearby: Boolean, val hostPerm: Boolean, val bt: Boolean, val wifi: Boolean,
    val cam: Boolean, val vpn: Boolean, val hotspot: Boolean, val battery: Boolean
)

private fun readSnap(ctx: Context) = Snap(
    nearby = Perms.has(ctx, Perms.bleClient()),
    hostPerm = Perms.has(ctx, Perms.hostAll()),
    bt = Net.bluetoothOn(ctx),
    wifi = Net.wifiEnabled(ctx),
    cam = Perms.camera(ctx),
    vpn = Net.isVpnActive(ctx),
    hotspot = Net.hotspotIp() != null,
    battery = Net.ignoringBattery(ctx)
)

private class Req(
    val icon: ImageVector, val title: String, val desc: String, val ok: Boolean,
    val actionLabel: String, val action: () -> Unit
)

/** One place that shows everything the app needs and fixes each item with a single tap. */
@Composable
fun SetupScreen(nav: Nav, actions: Actions) {
    val s = LocalStrings.current
    val hs = LocalHs.current
    val ctx = LocalContext.current
    val snap by produceState(initialValue = readSnap(ctx)) {
        while (true) { value = readSnap(ctx); delay(1000) }
    }

    val receiver = listOf(
        Req(Icons.Rounded.Shield, s.reqNearby, s.reqNearbyDesc, snap.nearby, s.fixGrant) { actions.requestPerms(Perms.bleClient()) { } },
        Req(Icons.Rounded.Bluetooth, s.reqBluetooth, s.reqBluetoothClientDesc, snap.bt, s.fixTurnOn) { actions.enableBluetooth() },
        Req(Icons.Rounded.Wifi, s.reqWifi, s.reqWifiDesc, snap.wifi, s.fixTurnOn) { Net.openWifiPanel(ctx) },
        Req(Icons.Rounded.QrCodeScanner, s.reqCamera, s.reqCameraDesc, snap.cam, s.fixGrant) { actions.requestPerms(arrayOf(Manifest.permission.CAMERA)) { } }
    )
    val host = listOf(
        Req(Icons.Rounded.Shield, s.reqHostPerm, s.reqHostPermDesc, snap.hostPerm, s.fixGrant) { actions.requestPerms(Perms.hostAll()) { } },
        Req(Icons.Rounded.Bluetooth, s.reqBluetooth, s.reqBluetoothHostDesc, snap.bt, s.fixTurnOn) { actions.enableBluetooth() },
        Req(Icons.Rounded.VpnKey, s.reqVpn, s.reqVpnDesc, snap.vpn, s.fixOpen) { Net.openVpnSettings(ctx) },
        Req(Icons.Rounded.WifiTethering, s.reqHotspot, s.reqHotspotDesc, snap.hotspot, s.fixOpen) { Net.openHotspotSettings(ctx) },
        Req(Icons.Rounded.BatteryChargingFull, s.reqBattery, s.reqBatteryDesc, snap.battery, s.fixOpen) { Net.openBatterySettings(ctx) }
    )

    Column(Modifier.fillMaxSize()) {
        TopBar(s.setupTitle, onBack = nav.back)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GlassCard(Modifier.fillMaxWidth().appear(0)) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    IconBubble(Icons.Rounded.Lightbulb, Amber, 40.dp)
                    Text(s.setupIntro, color = hs.sub, fontSize = 13.sp, lineHeight = 21.sp, modifier = Modifier.weight(1f))
                }
            }
            ReqGroup(s.setupReceiverTitle, receiver, 1)
            ReqGroup(s.setupHostTitle, host, 2)
        }
    }
}

@Composable
private fun ReqGroup(title: String, reqs: List<Req>, index: Int) {
    val s = LocalStrings.current
    val hs = LocalHs.current
    val done = reqs.count { it.ok }
    val all = done == reqs.size
    Column(Modifier.fillMaxWidth().appear(index), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = hs.text, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text(
                if (all) s.setupAllReady else "${s.n(done)}/${s.n(reqs.size)}",
                color = if (all) Mint else Amber, fontWeight = FontWeight.Bold, fontSize = 13.sp
            )
        }
        GlassCard(Modifier.fillMaxWidth()) {
            Column {
                reqs.forEachIndexed { i, r ->
                    if (i > 0) Box(Modifier.fillMaxWidth().padding(start = 68.dp).height(1.dp).background(hs.cardStroke.copy(alpha = 0.6f)))
                    ReqRow(r)
                }
            }
        }
    }
}

@Composable
private fun ReqRow(r: Req) {
    val hs = LocalHs.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconBubble(r.icon, if (r.ok) Mint else Amber, 40.dp)
        Column(Modifier.weight(1f)) {
            Text(r.title, color = hs.text, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(r.desc, color = hs.sub, fontSize = 12.sp, lineHeight = 17.sp)
        }
        if (r.ok) {
            Icon(Icons.Rounded.CheckCircle, null, tint = Mint, modifier = Modifier.size(26.dp))
        } else {
            Box(
                Modifier.pressable(onClick = r.action).clip(CircleShape).background(hs.brand).padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) { Text(r.actionLabel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1) }
        }
    }
}
