package ir.mrpc.hotshare.core

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.provider.Settings
import java.net.Inet4Address
import java.net.NetworkInterface

@Suppress("DEPRECATION")
object Net {
    private val tetherIf = Regex("^(ap\\d+|swlan\\d+|wlan[1-9]\\d*|softap\\d*|rndis\\d+|bt-pan|usb\\d+)$")

    /** IPv4 اینترفیس هات‌اسپات/تترینگ این گوشی (مثلاً 192.168.43.1)؛ اگر خاموش باشد null. */
    fun hotspotIp(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { tetherIf.matches(it.name) && it.isUp }
            .flatMap { it.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress }?.hostAddress
    }.getOrNull()

    /** روش دوم (reflection) برای رام‌هایی که اسم اینترفیسشان عجیب است. */
    fun hotspotFlag(ctx: Context): Boolean = runCatching {
        val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val m = WifiManager::class.java.getDeclaredMethod("isWifiApEnabled")
        m.isAccessible = true
        m.invoke(wm) as Boolean
    }.getOrDefault(false)

    fun isVpnActive(ctx: Context): Boolean {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        return cm.allNetworks.any { cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true }
    }

    /** آی‌پی گیت‌وی وای‌فای فعلی (= آی‌پی گوشی میزبان). */
    fun gatewayIp(ctx: Context): String? {
        val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val g = wm.dhcpInfo?.gateway ?: 0
        return if (g == 0) null else "${g and 255}.${(g shr 8) and 255}.${(g shr 16) and 255}.${(g shr 24) and 255}"
    }

    fun wifiNetwork(ctx: Context): Network? {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        return cm.allNetworks.firstOrNull {
            val c = cm.getNetworkCapabilities(it)
            c != null && c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) && !c.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        }
    }

    private fun open(ctx: Context, vararg intents: Intent) {
        for (i in intents) {
            try { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return } catch (_: Exception) {}
        }
    }

    fun openHotspotSettings(ctx: Context) = open(
        ctx,
        Intent().setClassName("com.android.settings", "com.android.settings.TetherSettings"),
        Intent(Settings.ACTION_WIRELESS_SETTINGS),
        Intent(Settings.ACTION_SETTINGS)
    )

    fun openWifiSettings(ctx: Context) = open(ctx, Intent(Settings.ACTION_WIFI_SETTINGS), Intent(Settings.ACTION_SETTINGS))

    fun openBatterySettings(ctx: Context) = open(ctx, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS), Intent(Settings.ACTION_SETTINGS))
}
