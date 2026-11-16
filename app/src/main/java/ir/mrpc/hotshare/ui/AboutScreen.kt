package ir.mrpc.hotshare.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val s = LocalStrings.current
    val ctx = LocalContext.current
    val ver = runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "1.0"
    AuroraBackground {
        Column(Modifier.fillMaxSize()) {
            TopBar(s.helpAbout, onBack, null)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Logo(Modifier.size(90.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(s.appName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Text("${s.version} $ver", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .6f))
                    Text(s.designedBy, fontSize = 13.sp, color = Cyan, fontWeight = FontWeight.Medium)
                }
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(s.howTitle, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(s.shareHeader, color = Cyan, fontWeight = FontWeight.Bold)
                    s.helpHost.forEachIndexed { i, t -> Text("${i + 1}. $t", Modifier.padding(top = 4.dp), fontSize = 14.sp) }
                    Spacer(Modifier.height(12.dp))
                    Text(s.receiveHeader, color = Violet, fontWeight = FontWeight.Bold)
                    s.helpClient.forEachIndexed { i, t -> Text("${i + 1}. $t", Modifier.padding(top = 4.dp), fontSize = 14.sp) }
                }
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(s.privacyTitle, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(s.privacy, fontSize = 14.sp, lineHeight = 22.sp)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
