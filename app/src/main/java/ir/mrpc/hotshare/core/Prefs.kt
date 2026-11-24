package ir.mrpc.hotshare.core

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object Prefs {
    private lateinit var sp: SharedPreferences

    var theme by mutableStateOf("system")          // system | light | dark
    var lang by mutableStateOf("fa")               // fa | en  (Persian is the default)
    var autoStart by mutableStateOf(true)
    var keepScreenOn by mutableStateOf(true)
    var autoHotspot by mutableStateOf(true)
    var autoApprove by mutableStateOf(false)
    var apSsid by mutableStateOf("")               // saved system-hotspot name (optional)
    var apPsk by mutableStateOf("")                // saved system-hotspot password (optional)
    var code by mutableStateOf("")
    var askedNotif = false
    /** Stable random id of THIS phone; sent to hosts so they can remember "always allow". */
    var deviceId = ""
    var trustedCount by mutableStateOf(0)
    private val trusted = java.util.concurrent.ConcurrentHashMap<String, String>()
    /** The one combined tutorial: shown automatically on the very first launch only. */
    var tutorialSeen = false
    /** When on (Menu -> General), the tutorial is shown every time the app opens. */
    var tutorialAtStart by mutableStateOf(false)

    fun init(ctx: Context) {
        sp = ctx.getSharedPreferences("hotshare", Context.MODE_PRIVATE)
        theme = sp.getString("theme", "system") ?: "system"
        lang = sp.getString("lang", "fa") ?: "fa"
        autoStart = sp.getBoolean("autoStart", true)
        keepScreenOn = sp.getBoolean("keepOn", true)
        autoHotspot = sp.getBoolean("autoHotspot", true)
        autoApprove = sp.getBoolean("autoApprove", false)
        apSsid = sp.getString("apSsid", "") ?: ""
        apPsk = sp.getString("apPsk", "") ?: ""
        askedNotif = sp.getBoolean("askedNotif", false)
        // an update of an install that already saw the old per-screen tour must not replay the tutorial
        tutorialSeen = sp.getBoolean("tutSeen", false) || sp.contains("tour_home")
        tutorialAtStart = sp.getBoolean("tutAtStart", false)
        code = sp.getString("code", null) ?: Pairing.newCode().also { sp.edit().putString("code", it).apply() }
        deviceId = sp.getString("deviceId", null)
            ?: java.util.UUID.randomUUID().toString().replace("-", "").take(12).also { sp.edit().putString("deviceId", it).apply() }
        sp.getStringSet("trusted", emptySet())?.forEach { e ->
            val i = e.indexOf('|')
            if (i > 0) trusted[e.substring(0, i)] = e.substring(i + 1)
        }
        trustedCount = trusted.size
    }

    fun isTrusted(id: String) = trusted.containsKey(id)
    fun trust(id: String, name: String) { trusted[id] = name; persistTrusted() }
    fun clearTrusted() { trusted.clear(); persistTrusted() }
    private fun persistTrusted() {
        sp.edit().putStringSet("trusted", trusted.map { "${it.key}|${it.value}" }.toSet()).apply()
        trustedCount = trusted.size
    }

    fun save() {
        sp.edit()
            .putString("theme", theme)
            .putString("lang", lang)
            .putBoolean("autoStart", autoStart)
            .putBoolean("keepOn", keepScreenOn)
            .putBoolean("autoHotspot", autoHotspot)
            .putBoolean("autoApprove", autoApprove)
            .putString("apSsid", apSsid)
            .putString("apPsk", apPsk)
            .putBoolean("askedNotif", askedNotif)
            .putBoolean("tutSeen", tutorialSeen)
            .putBoolean("tutAtStart", tutorialAtStart)
            .putString("code", code)
            .apply()
    }

    /** Should the tutorial open right after the splash screen? */
    fun showTutorialOnStart(): Boolean = !tutorialSeen || tutorialAtStart

    fun markTutorialSeen() { tutorialSeen = true; save() }

    fun newCode() {
        code = Pairing.newCode()
        save()
    }
}
