package ir.mrpc.hotshare.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HsSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val hs = LocalHs.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (hs.dark) Color(0xFF111833) else Color.White,
        contentColor = hs.text
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 24.dp).padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

/** 8-digit code entry with a custom animated keypad. */
@Composable
fun CodeSheet(onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    val s = LocalStrings.current
    val hs = LocalHs.current
    var code by remember { mutableStateOf("") }
    LaunchedEffect(code) { if (code.length == 8) onSubmit(code) }
    HsSheet(onDismiss) {
        Text(s.codeTitle, color = hs.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(s.codeSub, color = hs.sub, fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        DigitBoxes(code, boxWidth = 30.dp)
        Spacer(Modifier.height(8.dp))
        Keypad(
            onDigit = { if (code.length < 8) code += it },
            onBackspace = { if (code.isNotEmpty()) code = code.dropLast(1) }
        )
    }
}

/** Asks for the host hotspot name & password inside the app. */
@Composable
fun WifiSheet(knownSsid: String?, retry: Boolean, onSubmit: (String, String) -> Unit, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    val hs = LocalHs.current
    var ssid by remember { mutableStateOf(knownSsid.orEmpty()) }
    var pass by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    HsSheet(onDismiss) {
        Text(s.wifiSheetTitle, color = hs.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(s.wifiSheetSub, color = hs.sub, fontSize = 13.sp, textAlign = TextAlign.Center)
        if (retry) Text(s.wifiWrong, color = Rose, fontSize = 13.sp, textAlign = TextAlign.Center)
        OutlinedTextField(
            value = ssid, onValueChange = { ssid = it }, singleLine = true,
            label = { Text(s.wifiNetwork) }, modifier = Modifier.fillMaxWidth(),
            enabled = true
        )
        OutlinedTextField(
            value = pass, onValueChange = { pass = it }, singleLine = true,
            label = { Text(s.wifiPassword) }, modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Switch(show, { show = it })
            Spacer(Modifier.width(10.dp))
            Text(s.showPass, color = hs.sub, fontSize = 13.sp)
        }
        GradientButton(s.joinWifi, { onSubmit(ssid.trim(), pass) }, enabled = ssid.isNotBlank())
        GhostButton(s.cancel, onDismiss, Modifier.fillMaxWidth())
    }
}

/** Host-side hotspot details (name & password of the phone hotspot, stored once). */
@Composable
fun HotspotDetailsSheet(ssid0: String, psk0: String, onSave: (String, String) -> Unit, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    val hs = LocalHs.current
    var ssid by remember { mutableStateOf(ssid0) }
    var pass by remember { mutableStateOf(psk0) }
    HsSheet(onDismiss) {
        Text(s.hotspotDetails, color = hs.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(s.hotspotDetailsDesc, color = hs.sub, fontSize = 13.sp, textAlign = TextAlign.Center)
        OutlinedTextField(value = ssid, onValueChange = { ssid = it }, singleLine = true, label = { Text(s.wifiNetwork) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = pass, onValueChange = { pass = it }, singleLine = true, label = { Text(s.wifiPassword) }, modifier = Modifier.fillMaxWidth())
        GradientButton(s.save, { onSave(ssid.trim(), pass) })
    }
}
