package ir.mrpc.hotshare.core

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object Prefs {
    private lateinit var sp: SharedPreferences

    var language by mutableStateOf("fa")          // fa | en
    var theme by mutableStateOf("system")         // system | light | dark
    var autoStart by mutableStateOf(true)
    var keepScreenOn by mutableStateOf(true)
    var code by mutableStateOf("")
    var askedNotif = false

    fun init(ctx: Context) {
        sp = ctx.getSharedPreferences("hotshare", Context.MODE_PRIVATE)
        language = sp.getString("lang", "fa") ?: "fa"
        theme = sp.getString("theme", "system") ?: "system"
        autoStart = sp.getBoolean("autoStart", true)
        keepScreenOn = sp.getBoolean("keepOn", true)
        askedNotif = sp.getBoolean("askedNotif", false)
        code = sp.getString("code", null) ?: Pairing.newCode().also { sp.edit().putString("code", it).apply() }
    }

    fun save() {
        sp.edit()
            .putString("lang", language)
            .putString("theme", theme)
            .putBoolean("autoStart", autoStart)
            .putBoolean("keepOn", keepScreenOn)
            .putBoolean("askedNotif", askedNotif)
            .putString("code", code)
            .apply()
    }

    fun newCode() {
        code = Pairing.newCode()
        save()
    }
}
