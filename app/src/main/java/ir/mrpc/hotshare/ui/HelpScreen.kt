package ir.mrpc.hotshare.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Help = a short index. Each topic opens as its own page. */
@Composable
fun HelpScreen(nav: Nav) {
    val s = LocalStrings.current
    val hs = LocalHs.current
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text(s.howTitle, color = hs.text, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp).appear(0))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MenuCard(Icons.Rounded.PlayCircle, Amber, s.tutView, s.tutViewDesc, 1) { nav.go(Screen.ONBOARD) }
            MenuCard(Icons.Rounded.QuestionMark, Cyan, s.helpWhichTitle, s.helpWhichDesc, 2) { nav.go(Screen.HELP_WHICH) }
            MenuCard(Icons.Rounded.Sensors, Mint, s.helpMethodsTitle, s.helpMethodsDesc, 3) { nav.go(Screen.HELP_METHODS) }
            MenuCard(Icons.Rounded.Checklist, Violet, s.helpStepsTitle, s.helpStepsDesc, 4) { nav.go(Screen.HELP_STEPS) }
            MenuCard(Icons.Rounded.Wifi, Pink, s.helpWifiTitle, s.helpWifiDesc, 5) { nav.go(Screen.HELP_WIFI) }
            MenuCard(Icons.Rounded.Build, Rose, s.helpFaqTitle, null, 6) { nav.go(Screen.HELP_FAQ) }
        }
    }
}

@Composable
private fun HelpPage(title: String, nav: Nav, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBar(title, onBack = nav.back)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}

@Composable
fun HelpWhichScreen(nav: Nav) {
    val s = LocalStrings.current
    HelpPage(s.helpWhichTitle, nav) {
        ChoiceCard(Icons.Rounded.Share, Cyan, s.helpHostCardTitle, s.helpHostCardBody, s.helpGoHost, 0) { nav.go(Screen.HOST) }
        ChoiceCard(Icons.Rounded.Download, Violet, s.helpClientCardTitle, s.helpClientCardBody, s.helpGoClient, 1) { nav.go(Screen.RECEIVE) }
    }
}

@Composable
fun HelpMethodsScreen(nav: Nav) {
    val s = LocalStrings.current
    HelpPage(s.helpMethodsTitle, nav) {
        GlassCard(Modifier.fillMaxWidth().appear(0)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MethodRow(Icons.Rounded.Bluetooth, Mint, s.helpMethodRadar, s.helpMethodRadarDesc)
                MethodRow(Icons.Rounded.QrCodeScanner, Pink, s.helpMethodQr, s.helpMethodQrDesc)
                MethodRow(Icons.Rounded.Keyboard, Amber, s.helpMethodCode, s.helpMethodCodeDesc)
            }
        }
    }
}

@Composable
fun HelpStepsScreen(nav: Nav) {
    val s = LocalStrings.current
    HelpPage(s.helpStepsTitle, nav) {
        StepsCard(s.shareHeader, s.helpHost, Cyan, 0)
        StepsCard(s.clientHeader, s.helpClient, Violet, 1)
    }
}

@Composable
fun HelpWifiScreen(nav: Nav) {
    val s = LocalStrings.current
    HelpPage(s.helpWifiTitle, nav) {
        StepsCard(s.helpWifiTitle, s.helpWifiBody, Pink, 0)
    }
}

@Composable
fun HelpFaqScreen(nav: Nav) {
    val s = LocalStrings.current
    val hs = LocalHs.current
    HelpPage(s.helpFaqTitle, nav) {
        GlassCard(Modifier.fillMaxWidth().appear(0)) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
                s.helpFaq.forEachIndexed { i, (q, a) ->
                    if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(hs.cardStroke.copy(alpha = 0.6f)))
                    FaqItem(q, a)
                }
            }
        }
    }
}

@Composable
private fun ChoiceCard(icon: ImageVector, color: Color, title: String, body: String, button: String, index: Int, onClick: () -> Unit) {
    val hs = LocalHs.current
    GlassCard(Modifier.fillMaxWidth().appear(index)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBubble(icon, color, 42.dp)
                Text(title, color = hs.text, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, lineHeight = 22.sp, modifier = Modifier.weight(1f))
            }
            Text(body, color = hs.sub, fontSize = 13.sp, lineHeight = 21.sp)
            GhostButton(button, onClick, Modifier.fillMaxWidth(), tint = color)
        }
    }
}

@Composable
private fun MethodRow(icon: ImageVector, color: Color, title: String, desc: String) {
    val hs = LocalHs.current
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        IconBubble(icon, color, 40.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = hs.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(desc, color = hs.sub, fontSize = 13.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun StepsCard(title: String, steps: List<String>, color: Color, index: Int) {
    val s = LocalStrings.current
    val hs = LocalHs.current
    GlassCard(Modifier.fillMaxWidth().appear(index)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(title, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            steps.forEachIndexed { i, t ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(28.dp).clip(CircleShape).background(color.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                        Text(s.n(i + 1), color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Text(t, color = hs.text, fontSize = 14.sp, lineHeight = 22.sp, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun FaqItem(question: String, answer: String) {
    val hs = LocalHs.current
    var open by remember { mutableStateOf(false) }
    val rot by animateFloatAsState(if (open) 180f else 0f, label = "faqRot")
    Column(
        Modifier.fillMaxWidth().pressable { open = !open }.animateContentSize().padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(question, color = hs.text, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.KeyboardArrowDown, null, tint = hs.sub, modifier = Modifier.graphicsLayer { rotationZ = rot })
        }
        if (open) Text(answer, color = hs.sub, fontSize = 13.sp, lineHeight = 21.sp)
    }
}

@Composable
fun PrivacyScreen(nav: Nav) {
    val s = LocalStrings.current
    val hs = LocalHs.current
    Column(Modifier.fillMaxSize()) {
        TopBar(s.privacyTitle, onBack = nav.back)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GlassCard(Modifier.fillMaxWidth().appear(0)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IconBubble(Icons.Rounded.Security, Mint, 38.dp)
                        Text(s.privacyIntro, color = hs.text, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    }
                    Text(s.privacy, color = hs.sub, fontSize = 14.sp, lineHeight = 23.sp)
                }
            }
        }
    }
}

@Composable
fun AboutScreen(nav: Nav) {
    val s = LocalStrings.current
    val hs = LocalHs.current
    val ctx = LocalContext.current
    val ver = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "" }
    Column(Modifier.fillMaxSize()) {
        TopBar(s.aboutTitle, onBack = nav.back)
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)
        ) {
            Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                PulseRings(Modifier.fillMaxSize(), Violet)
                OrbitRings(Modifier.size(170.dp))
                BrandMark(80.dp)
            }
            Text("HotShare", color = hs.text, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Text(s.tagline, color = hs.sub, fontSize = 14.sp, textAlign = TextAlign.Center)
            Text("${s.version} $ver", color = hs.sub, fontSize = 13.sp)
            Text(s.designedBy, color = hs.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}
