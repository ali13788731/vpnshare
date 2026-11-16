package ir.mrpc.hotshare.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RoleScreen(nav: (Screen) -> Unit) {
    val s = LocalStrings.current
    AuroraBackground {
        Column(Modifier.fillMaxSize()) {
            TopBar(s.appName, null, nav)
            Column(Modifier.weight(1f).padding(horizontal = 20.dp), verticalArrangement = Arrangement.Center) {
                Appear(0) { Logo(Modifier.size(84.dp).align(Alignment.CenterHorizontally)) }
                Spacer(Modifier.height(20.dp))
                Appear(120) {
                    Text(s.chooseRole, Modifier.fillMaxWidth(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(20.dp))
                Appear(260) { RoleCard(s.shareTitle, s.shareDesc, Icons.Rounded.WifiTethering, listOf(Cyan, Color(0xFF3B82F6))) { nav(Screen.HOST) } }
                Spacer(Modifier.height(16.dp))
                Appear(400) { RoleCard(s.receiveTitle, s.receiveDesc, Icons.Rounded.QrCodeScanner, listOf(Violet, Color(0xFFEC4899))) { nav(Screen.CLIENT) } }
            }
            Text(s.designedBy, Modifier.align(Alignment.CenterHorizontally).navigationBarsPadding().padding(bottom = 16.dp),
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .5f))
        }
    }
}

@Composable
private fun RoleCard(title: String, desc: String, icon: ImageVector, accent: List<Color>, onClick: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth(), onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(accent)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(desc, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Cyan)
        }
    }
}
