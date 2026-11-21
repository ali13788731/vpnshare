package ir.mrpc.hotshare.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import ir.mrpc.hotshare.core.Prefs

@Composable
fun MenuScreen(nav: Nav, actions: Actions, onCode: () -> Unit) {
    val s = LocalStrings.current
    val hs = LocalHs.current
    val ctx = LocalContext.current
    var showAp by remember { mutableStateOf(false) }
    if (showAp) HotspotDetailsSheet(Prefs.apSsid, Prefs.apPsk, { a, b -> Prefs.apSsid = a; Prefs.apPsk = b; Prefs.save(); showAp = false }, { showAp = false })

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text(s.menuTitle, color = hs.text, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp).appear(0))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── 1. Quick start
            SectionHeader(s.secStart)
            Row(Modifier.fillMaxWidth().appear(1), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile(Icons.Rounded.Share, s.shareTitle, Cyan, Modifier.weight(1f)) { nav.go(Screen.HOST) }
                QuickTile(Icons.Rounded.Download, s.receiveTitle, Violet, Modifier.weight(1f)) { nav.go(Screen.RECEIVE) }
            }
            Row(Modifier.fillMaxWidth().appear(2), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile(Icons.Rounded.QrCodeScanner, s.scanQr, Pink, Modifier.weight(1f)) { nav.go(Screen.SCAN) }
                QuickTile(Icons.Rounded.Keyboard, s.enterCode, Amber, Modifier.weight(1f), onCode)
            }

            // ── 2. Get the phone ready (Bluetooth / Wi-Fi / permissions / VPN)
            MenuGroup(3) {
                NavRow(Icons.Rounded.Settings, Mint, s.setupTitle, s.setupRowDesc) { nav.go(Screen.SETUP) }
            }

            // ── 3. Sharing settings
            SectionHeader(s.secSharing)
            MenuGroup(4) {
                SwitchRow(Icons.Rounded.WifiTethering, Cyan, s.autoHotspot, s.autoHotspotDesc, Prefs.autoHotspot) { Prefs.autoHotspot = it; Prefs.save() }
                Divider()
                SwitchRow(Icons.Rounded.PlayArrow, Mint, s.autoStart, s.autoStartDesc, Prefs.autoStart) { Prefs.autoStart = it; Prefs.save() }
                Divider()
                SwitchRow(Icons.Rounded.DoneAll, Violet, s.autoApprove, s.autoApproveDesc, Prefs.autoApprove) { Prefs.autoApprove = it; Prefs.save() }
                Divider()
                NavRow(Icons.Rounded.Router, Amber, s.hotspotDetails, s.hotspotDetailsDesc) { showAp = true }
                Divider()
                NavRow(Icons.Rounded.VpnKey, Pink, s.regen, s.regenDesc) { Prefs.newCode() }
            }

            // ── 4. Language & look
            SectionHeader(s.secLook)
            MenuGroup(5) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        IconBubble(Icons.Rounded.Language, Cyan, 40.dp)
                        Text(s.language, color = hs.text, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    }
                    Segmented(listOf("fa" to s.langFa, "en" to s.langEn), Prefs.lang) { Prefs.lang = it; Prefs.save() }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        IconBubble(Icons.Rounded.Palette, Violet, 40.dp)
                        Text(s.theme, color = hs.text, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    }
                    Segmented(listOf("system" to s.themeSystem, "light" to s.themeLight, "dark" to s.themeDark), Prefs.theme) { Prefs.theme = it; Prefs.save() }
                }
            }

            // ── 5. Device
            SectionHeader(s.secDevice)
            MenuGroup(6) {
                SwitchRow(Icons.Rounded.ScreenLockPortrait, Cyan, s.keepOn, s.keepOnDesc, Prefs.keepScreenOn) { Prefs.keepScreenOn = it; Prefs.save() }
                Divider()
                NavRow(Icons.Rounded.BatteryChargingFull, Mint, s.battery, s.batteryDesc) { Net.openBatterySettings(ctx) }
            }

            // ── 6. Help & support
            SectionHeader(s.secHelpGroup)
            MenuGroup(7) {
                NavRow(Icons.Rounded.Lightbulb, Amber, s.howTitle, null) { nav.tab(Screen.HELP) }
                Divider()
                NavRow(Icons.Rounded.Visibility, Cyan, s.tourStart, s.tourStartDesc) { nav.tour("home") }
                Divider()
                NavRow(Icons.Rounded.Security, Mint, s.privacyTitle, null) { nav.go(Screen.PRIVACY) }
                Divider()
                NavRow(Icons.Rounded.Info, Violet, s.aboutTitle, null) { nav.go(Screen.ABOUT) }
            }
        }
    }
}

@Composable
private fun MenuGroup(index: Int, content: @Composable ColumnScope.() -> Unit) {
    GlassCard(Modifier.fillMaxWidth().appear(index)) { Column(content = content) }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().padding(start = 70.dp).height(1.dp).background(LocalHs.current.cardStroke.copy(alpha = 0.6f)))
}

@Composable
private fun QuickTile(icon: ImageVector, label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    val hs = LocalHs.current
    GlassCard(modifier, radius = 22.dp, onClick = onClick) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBubble(icon, color, 44.dp)
            Text(label, color = hs.text, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
        }
    }
}

@Composable
private fun RowShell(icon: ImageVector, color: Color, title: String, desc: String?, trailing: @Composable () -> Unit, onClick: (() -> Unit)?) {
    val hs = LocalHs.current
    Row(
        Modifier.fillMaxWidth().let { if (onClick != null) it.pressable(onClick = onClick) else it }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        IconBubble(icon, color, 40.dp)
        Column(Modifier.weight(1f)) {
            Text(title, color = hs.text, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            if (desc != null) Text(desc, color = hs.sub, fontSize = 12.sp, lineHeight = 17.sp)
        }
        trailing()
    }
}

@Composable
private fun NavRow(icon: ImageVector, color: Color, title: String, desc: String?, onClick: () -> Unit) {
    RowShell(icon, color, title, desc, { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = LocalHs.current.sub) }, onClick)
}

@Composable
private fun SwitchRow(icon: ImageVector, color: Color, title: String, desc: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    RowShell(icon, color, title, desc, {
        Switch(
            checked, onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Mint, checkedThumbColor = Color.White, uncheckedTrackColor = LocalHs.current.cardStroke, uncheckedThumbColor = Color.White, uncheckedBorderColor = Color.Transparent)
        )
    }) { onChange(!checked) }
}

@Composable
private fun Segmented(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    val hs = LocalHs.current
    val idx = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    BoxWithConstraints(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(hs.cardStroke.copy(alpha = 0.45f)).padding(4.dp)) {
        val w = maxWidth / options.size
        val x by animateDpAsState(w * idx, spring(0.7f, 400f), label = "segX")
        Box(Modifier.offset(x = x).width(w).height(40.dp).clip(RoundedCornerShape(13.dp)).background(hs.brand))
        Row {
            options.forEach { (key, label) ->
                val sel = key == selected
                val c by animateColorAsState(if (sel) Color.White else hs.sub, label = "segC")
                Box(Modifier.width(w).height(40.dp).pressable { onSelect(key) }, contentAlignment = Alignment.Center) {
                    Text(label, color = c, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium, fontSize = 14.sp)
                }
            }
        }
    }
}
