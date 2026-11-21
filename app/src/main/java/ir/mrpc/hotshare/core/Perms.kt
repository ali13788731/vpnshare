package ir.mrpc.hotshare.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object Perms {
    /** Needed on the receiving phone to find hosts over Bluetooth LE. */
    fun bleClient(): Array<String> =
        if (Build.VERSION.SDK_INT >= 31) arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

    /** Needed on the host to be visible over Bluetooth LE. */
    fun bleHost(): Array<String> =
        if (Build.VERSION.SDK_INT >= 31) arrayOf(Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT)
        else emptyArray()

    /** Needed to create the app's own (local-only) hotspot. */
    fun hotspot(): Array<String> =
        if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES)
        else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

    /** Needed to list nearby Wi-Fi networks (same permissions as the hotspot). */
    fun wifiScan(): Array<String> = hotspot()

    fun hostAll(): Array<String> {
        val l = ArrayList<String>()
        l += bleHost(); l += hotspot()
        if (Build.VERSION.SDK_INT >= 33) l += Manifest.permission.POST_NOTIFICATIONS
        return l.distinct().toTypedArray()
    }

    fun has(ctx: Context, perms: Array<String>) =
        perms.all { ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED }

    fun camera(ctx: Context) = has(ctx, arrayOf(Manifest.permission.CAMERA))
}
