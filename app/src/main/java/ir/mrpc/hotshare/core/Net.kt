package ir.mrpc.hotshare.core

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import java.net.Inet4Address
import java.net.NetworkInterface

@Suppress("DEPRECATION")
object Net {
    private val tetherIf = Regex("^(ap\\d+|swlan\\d+|wlan[1-9]\\d*|softap\\d*|rndis\\d+|bt-pan|usb\\d+|p2p-wlan\\d.*)$")
    private val ignoreIf = Regex("^(lo|wlan0|rmnet.*|ccmni.*|tun.*|dummy.*|v4-.*|ifb.*|sit.*|ip6.*|p2p0|eth\\d*)$")

    /** Friendly name of this phone (what the user sees in Settings), falling back to the model. */
    fun deviceName(ctx: Context): String {
        val n = runCatching { Settings.Global.getString(ctx.contentResolver, "device_name") }.getOrNull()
        return (n?.takeIf { it.isNotBlank() } ?: Build.MODEL ?: "Android").trim()
    }

    /** IPv4 of this phone's hotspot/tethering interface (e.g. 192.168.43.1); null when off. */
    fun hotspotIp(): String? = runCatching {
        val all = NetworkInterface.getNetworkInterfaces().toList().filter { it.isUp }
        fun ipOf(i: NetworkInterface) = i.inetAddresses.toList().filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress }?.hostAddress
        all.filter { tetherIf.matches(it.name) }.firstNotNullOfOrNull { ipOf(it) }
            ?: all.filter { !ignoreIf.matches(it.name) }
                .firstNotNullOfOrNull { i ->
                    i.inetAddresses.toList().filterIsInstance<Inet4Address>()
                        .firstOrNull { it.isSiteLocalAddress }?.hostAddress
                }
    }.getOrNull()

    /** Second detection method (reflection) for ROMs with unusual interface names. */
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

    /** Gateway of the current Wi-Fi (the host phone's IP when joined to its hotspot). */
    fun gatewayIp(ctx: Context): String? = runCatching {
        val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val g = wm.dhcpInfo?.gateway ?: 0
        if (g == 0) null else "${g and 255}.${(g shr 8) and 255}.${(g shr 16) and 255}.${(g shr 24) and 255}"
    }.getOrNull()

    /** Gateway of a specific network (used for networks joined through WifiNetworkSpecifier). */
    fun gatewayOf(ctx: Context, n: Network): String? = runCatching {
        val lp = ctx.getSystemService(ConnectivityManager::class.java).getLinkProperties(n) ?: return null
        lp.routes.firstOrNull { it.isDefaultRoute && it.gateway is Inet4Address }?.gateway?.hostAddress
            ?: lp.linkAddresses.firstOrNull { it.address is Inet4Address }?.address?.hostAddress
                ?.let { it.substringBeforeLast('.') + ".1" }
    }.getOrNull()

    fun hasIpv4(ctx: Context, n: Network): Boolean = runCatching {
        ctx.getSystemService(ConnectivityManager::class.java).getLinkProperties(n)
            ?.linkAddresses?.any { it.address is Inet4Address } == true
    }.getOrDefault(false)

    fun wifiNetwork(ctx: Context): Network? {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        return cm.allNetworks.firstOrNull {
            val c = cm.getNetworkCapabilities(it)
            c != null && c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) && !c.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        }
    }

    fun isWifiConnected(ctx: Context) = wifiNetwork(ctx) != null

    fun bluetoothOn(ctx: Context): Boolean = runCatching {
        ctx.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true
    }.getOrDefault(false)

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

    fun openBluetoothSettings(ctx: Context) = open(ctx, Intent(Settings.ACTION_BLUETOOTH_SETTINGS), Intent(Settings.ACTION_SETTINGS))

    fun openVpnSettings(ctx: Context) = open(
        ctx,
        Intent(Settings.ACTION_VPN_SETTINGS),
        Intent(Settings.ACTION_WIRELESS_SETTINGS),
        Intent(Settings.ACTION_SETTINGS)
    )

    fun wifiEnabled(ctx: Context): Boolean = runCatching {
        (ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).isWifiEnabled
    }.getOrDefault(false)

    /**
     * Android 10+ doesn't let apps toggle Wi-Fi, but it does allow the small in-app Wi-Fi panel
     * (one tap to switch on). Older versions can be switched on directly.
     */
    fun openWifiPanel(ctx: Context) {
        if (Build.VERSION.SDK_INT < 29) {
            val ok = runCatching {
                (ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).setWifiEnabled(true)
            }.getOrDefault(false)
            if (!ok) openWifiSettings(ctx)
            return
        }
        open(ctx, Intent(Settings.Panel.ACTION_WIFI), Intent(Settings.ACTION_WIFI_SETTINGS), Intent(Settings.ACTION_SETTINGS))
    }

    fun ignoringBattery(ctx: Context): Boolean = runCatching {
        (ctx.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(ctx.packageName)
    }.getOrDefault(false)
}
