package ir.mrpc.hotshare.core

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat

/** One nearby Wi-Fi network (several access points with the same name are merged). */
data class WifiNet(val ssid: String, val level: Int, val secured: Boolean) {
    /** 0..4 bars */
    val bars: Int get() = when {
        level >= -55 -> 4
        level >= -65 -> 3
        level >= -75 -> 2
        level >= -85 -> 1
        else -> 0
    }
}

/** Thin wrapper over WifiManager scanning; the UI polls it while the Wi-Fi picker is visible. */
@Suppress("DEPRECATION")
object WifiScan {
    private fun wm(ctx: Context) = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    /** Asks Android for a fresh scan. Android throttles this (a few per 2 minutes), so a refused call is normal. */
    @SuppressLint("MissingPermission")
    fun kick(ctx: Context): Boolean = runCatching { wm(ctx).startScan() }.getOrDefault(false)

    /** Calls [onUpdate] every time Android finishes a scan. Unregister the returned receiver when done. */
    fun listen(ctx: Context, onUpdate: () -> Unit): BroadcastReceiver {
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) = onUpdate()
        }
        ContextCompat.registerReceiver(
            ctx.applicationContext, r, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION), ContextCompat.RECEIVER_EXPORTED
        )
        return r
    }

    /** Latest known scan results, strongest first. Empty without permission. */
    @SuppressLint("MissingPermission")
    fun read(ctx: Context): List<WifiNet> = runCatching {
        wm(ctx).scanResults
            .mapNotNull { r ->
                val name = r.SSID?.trim('"')
                if (name.isNullOrBlank() || name == "<unknown ssid>" || name == "0x") null
                else {
                    val c = r.capabilities.orEmpty()
                    val secured = listOf("WPA", "WEP", "SAE", "PSK", "EAP").any { c.contains(it) }
                    WifiNet(name, r.level, secured)
                }
            }
            .groupBy { it.ssid }
            .map { (_, list) -> list.maxByOrNull { it.level }!! }
            .sortedByDescending { it.level }
    }.getOrDefault(emptyList())
}
