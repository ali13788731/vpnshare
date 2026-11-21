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

/** A big tappable card that opens its own page (used by the Menu and the Help index). */
@Composable
fun MenuCard(icon: ImageVector, color: Color, title: String, desc: String?, index: Int, onClick: () -> Unit) {
    val hs = LocalHs.current
    GlassCard(Modifier.fillMaxWidth().appear(index), onClick = onClick) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IconBubble(icon, color, 48.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = hs.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (desc != null) Text(desc, color = hs.sub, fontSize = 12.sp, lineHeight = 17.sp)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = hs.sub)
        }
    }
}

/** Menu = a short list of topics. Each one opens its own page. */
@Composable
fun MenuScreen(nav: Nav) {
    val s = LocalStrings.current
    val hs = LocalHs.current
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text(s.menuTitle, color = hs.text, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp).appear(0))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MenuCard(Icons.Rounded.Settings, Mint, s.setupTitle, s.menuSetupDesc, 1) { nav.go(Screen.SETUP) }
            MenuCard(Icons.Rounded.WifiTethering, Cyan, s.menuShare, s.menuShareDesc, 2) { nav.go(Screen.SET_SHARE) }
            MenuCard(Icons.Rounded.Palette, Violet, s.secLook, s.menuLookDesc, 3) { nav.go(Screen.SET_LOOK) }
            MenuCard(Icons.Rounded.Tune, Amber, s.menuGeneral, s.menuGeneralDesc, 4) { nav.go(Screen.SET_GENERAL) }
            MenuCard(Icons.Rounded.Security, Pink, s.privacyTitle, null, 5) { nav.go(Screen.PRIVACY) }
            MenuCard(Icons.Rounded.Info, Cyan, s.aboutTitle, null, 6) { nav.go(Screen.ABOUT) }
        }
    }
}

@Composable
private fun SettingsPage(title: String, nav: Nav, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBar(title, onBack = nav.back)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

/** Menu -> Sharing settings */
@Composable
fun ShareSettingsScreen(nav: Nav) {
    val s = LocalStrings.current
    var showAp by remember { mutableStateOf(false) }
    if (showAp) HotspotDetailsSheet(Prefs.apSsid, Prefs.apPsk, { a, b -> Prefs.apSsid = a; Prefs.apPsk = b; Prefs.save(); showAp = false }, { showAp = false })
    SettingsPage(s.menuShare, nav) {
        MenuGroup(0) {
            SwitchRow(Icons.Rounded.WifiTethering, Cyan, s.autoHotspot, s.autoHotspotDesc, Prefs.autoHotspot) { Prefs.autoHotspot = it; Prefs.save() }
            Divider()
            SwitchRow(Icons.Rounded.PlayArrow, Mint, s.autoStart, s.autoStartDesc, Prefs.autoStart) { Prefs.autoStart = it; Prefs.save() }
            Divider()
            SwitchRow(Icons.Rounded.DoneAll, Violet, s.autoApprove, s.autoApproveDesc, Prefs.autoApprove) { Prefs.autoApprove = it; Prefs.save() }
        }
        MenuGroup(1) {
            NavRow(Icons.Rounded.Router, Amber, s.hotspotDetails, s.hotspotDetailsDesc) { showAp = true }
            Divider()
            NavRow(Icons.Rounded.VpnKey, Pink, s.regen, s.regenDesc) { Prefs.newCode() }
        }
    }
}

/** Menu -> Language & look */
@Composable
fun LookSettingsScreen(nav: Nav) {
    val s = LocalStrings.current
    val hs = LocalHs.current
    SettingsPage(s.secLook, nav) {
        MenuGroup(0) {
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
    }
}

/** Menu -> General (screen, battery, tutorial) */
@Composable
fun GeneralSettingsScreen(nav: Nav) {
    val s = LocalStrings.current
    val ctx = LocalContext.current
    SettingsPage(s.menuGeneral, nav) {
        MenuGroup(0) {
            SwitchRow(Icons.Rounded.ScreenLockPortrait, Cyan, s.keepOn, s.keepOnDesc, Prefs.keepScreenOn) { Prefs.keepScreenOn = it; Prefs.save() }
            Divider()
            NavRow(Icons.Rounded.BatteryChargingFull, Mint, s.battery, s.batteryDesc) { Net.openBatterySettings(ctx) }
        }
        MenuGroup(1) {
            SwitchRow(Icons.Rounded.Lightbulb, Amber, s.tutAtStart, s.tutAtStartDesc, Prefs.tutorialAtStart) { Prefs.tutorialAtStart = it; Prefs.save() }
            Divider()
            NavRow(Icons.Rounded.PlayCircle, Violet, s.tutView, s.tutViewDesc) { nav.go(Screen.ONBOARD) }
        }
    }
}

@Composable
internal fun MenuGroup(index: Int, content: @Composable ColumnScope.() -> Unit) {
    GlassCard(Modifier.fillMaxWidth().appear(index)) { Column(content = content) }
}

@Composable
internal fun Divider() {
    Box(Modifier.fillMaxWidth().padding(start = 70.dp).height(1.dp).background(LocalHs.current.cardStroke.copy(alpha = 0.6f)))
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
internal fun NavRow(icon: ImageVector, color: Color, title: String, desc: String?, onClick: () -> Unit) {
    RowShell(icon, color, title, desc, { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = LocalHs.current.sub) }, onClick)
}

@Composable
internal fun SwitchRow(icon: ImageVector, color: Color, title: String, desc: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    RowShell(icon, color, title, desc, {
        Switch(
            checked, onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Mint, checkedThumbColor = Color.White, uncheckedTrackColor = LocalHs.current.cardStroke, uncheckedThumbColor = Color.White, uncheckedBorderColor = Color.Transparent)
        )
    }) { onChange(!checked) }
}

@Composable
internal fun Segmented(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
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
