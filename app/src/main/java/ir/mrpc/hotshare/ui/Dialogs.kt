package ir.mrpc.hotshare.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun ApprovalDialog(name: String, onAllow: () -> Unit, onDeny: () -> Unit) {
    val s = LocalStrings.current
    val hs = LocalHs.current
    Dialog(onDismissRequest = onDeny) {
        Column(
            Modifier.clip(RoundedCornerShape(30.dp)).background(if (hs.dark) Color(0xFF111833) else Color.White).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                PulseRings(Modifier.fillMaxSize(), Cyan, rings = 2)
                BrandMark(70.dp)
            }
            Text(s.approveTitle, color = hs.sub, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(name, color = hs.text, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Text(s.approveBody, color = hs.sub, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            GradientButton(s.allow, onAllow)
            GhostButton(s.deny, onDeny, Modifier.fillMaxWidth(), tint = Rose)
        }
    }
}
