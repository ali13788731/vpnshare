package ir.mrpc.hotshare.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.mrpc.hotshare.core.Net
import ir.mrpc.hotshare.core.Prefs

@Composable
fun SettingsScreen(onBack: () -> Unit, onNewCode: () -> Unit) {
    val s = LocalStrings.current
    val ctx = LocalContext.current
    AuroraBackground {
        Column(Modifier.fillMaxSize()) {
            TopBar(s.settings, onBack, null)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(s.language, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(Prefs.language == "fa", { Prefs.language = "fa"; Prefs.save() }, { Text("فارسی") })
                        FilterChip(Prefs.language == "en", { Prefs.language = "en"; Prefs.save() }, { Text("English") })
                    }
                }
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(s.theme, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(Prefs.theme == "system", { Prefs.theme = "system"; Prefs.save() }, { Text(s.themeSystem) })
                        FilterChip(Prefs.theme == "light", { Prefs.theme = "light"; Prefs.save() }, { Text(s.themeLight) })
                        FilterChip(Prefs.theme == "dark", { Prefs.theme = "dark"; Prefs.save() }, { Text(s.themeDark) })
                    }
                }
                GlassCard(Modifier.fillMaxWidth()) {
                    SwitchRow(s.autoStart, Prefs.autoStart) { Prefs.autoStart = it; Prefs.save() }
                    Spacer(Modifier.height(8.dp))
                    SwitchRow(s.keepOn, Prefs.keepScreenOn) { Prefs.keepScreenOn = it; Prefs.save() }
                }
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(s.battery, fontWeight = FontWeight.Bold)
                    Text(s.batteryDesc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton({ Net.openBatterySettings(ctx) }, Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.BatteryChargingFull, null); Spacer(Modifier.width(8.dp)); Text(s.battery, maxLines = 1)
                    }
                }
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(s.regen, fontWeight = FontWeight.Bold)
                    Text(s.regenDesc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onNewCode, Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Refresh, null); Spacer(Modifier.width(8.dp)); Text(s.regen)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), fontSize = 14.sp)
        Switch(checked, onChange)
    }
}
