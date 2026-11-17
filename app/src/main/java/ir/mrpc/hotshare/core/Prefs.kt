package ir.mrpc.hotshare.core

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object Prefs {
    private lateinit var sp: SharedPreferences

    var theme by mutableStateOf("system")          // system | light | dark
    var autoStart by mutableStateOf(true)
    var keepScreenOn by mutableStateOf(true)
    var autoHotspot by mutableStateOf(true)
    var autoApprove by mutableStateOf(false)
    var apSsid by mutableStateOf("")               // saved system-hotspot name (optional)
    var apPsk by mutableStateOf("")                // saved system-hotspot password (optional)
    var code by mutableStateOf("")
    var askedNotif = false

    fun init(ctx: Context) {
        sp = ctx.getSharedPreferences("hotshare", Context.MODE_PRIVATE)
        theme = sp.getString("theme", "system") ?: "system"
        autoStart = sp.getBoolean("autoStart", true)
        keepScreenOn = sp.getBoolean("keepOn", true)
        autoHotspot = sp.getBoolean("autoHotspot", true)
        autoApprove = sp.getBoolean("autoApprove", false)
        apSsid = sp.getString("apSsid", "") ?: ""
        apPsk = sp.getString("apPsk", "") ?: ""
        askedNotif = sp.getBoolean("askedNotif", false)
        code = sp.getString("code", null) ?: Pairing.newCode().also { sp.edit().putString("code", it).apply() }
    }

    fun save() {
        sp.edit()
            .putString("theme", theme)
            .putBoolean("autoStart", autoStart)
            .putBoolean("keepOn", keepScreenOn)
            .putBoolean("autoHotspot", autoHotspot)
            .putBoolean("autoApprove", autoApprove)
            .putString("apSsid", apSsid)
            .putString("apPsk", apPsk)
            .putBoolean("askedNotif", askedNotif)
            .putString("code", code)
            .apply()
    }

    fun newCode() {
        code = Pairing.newCode()
        save()
    }
}
