package ir.mrpc.hotshare.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext

@Composable
fun HelpScreen() {
    val s = LocalStrings.current
    val hs = LocalHs.current
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text(s.howTitle, color = hs.text, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp).appear(0))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StepsCard(s.shareHeader, s.helpHost, Cyan, 1)
            StepsCard(s.clientHeader, s.helpClient, Violet, 2)
            GlassCard(Modifier.fillMaxWidth().appear(3)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IconBubble(Icons.Rounded.Security, Mint, 38.dp)
                        Text(s.privacyTitle, color = hs.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Text(s.privacy, color = hs.sub, fontSize = 13.sp, lineHeight = 21.sp)
                }
            }
        }
    }
}

@Composable
private fun StepsCard(title: String, steps: List<String>, color: Color, index: Int) {
    val hs = LocalHs.current
    GlassCard(Modifier.fillMaxWidth().appear(index)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(title, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            steps.forEachIndexed { i, t ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(28.dp).clip(CircleShape).background(color.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                        Text((i + 1).toString(), color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Text(t, color = hs.text, fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.weight(1f))
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
