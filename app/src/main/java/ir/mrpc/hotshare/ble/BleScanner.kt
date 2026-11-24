package ir.mrpc.hotshare.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.ConcurrentHashMap

/** Looks for nearby phones that are sharing; nearest (strongest signal) first. */
@SuppressLint("MissingPermission")
class BleScanner(private val ctx: Context) {
    val devices = MutableStateFlow<List<NearbyDevice>>(emptyList())

    private val found = ConcurrentHashMap<String, NearbyDevice>()
    private val handles = ConcurrentHashMap<String, BluetoothDevice>()
    private var running = false

    private val cb = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, r: ScanResult) {
            val addr = r.device.address ?: return
            val raw = r.scanRecord?.getManufacturerSpecificData(BleProto.COMPANY_ID)
            val name = raw?.toString(Charsets.UTF_8)?.trim()?.takeIf { it.isNotEmpty() }
                ?: runCatching { r.device.name }.getOrNull() ?: "Device"
            val prev = found[addr]
            val rssi = if (prev == null) r.rssi.toFloat() else prev.rssi * 0.6f + r.rssi * 0.4f
            found[addr] = NearbyDevice(addr, name, rssi, System.currentTimeMillis())
            handles[addr] = r.device
            publish()
        }
    }

    private fun publish() {
        val now = System.currentTimeMillis()
        found.entries.removeIf { now - it.value.lastSeen > 10_000 }
        devices.value = found.values.sortedByDescending { it.rssi }
    }

    /** Called periodically by the UI so stale devices disappear even when no new results arrive. */
    fun prune() = publish()

    fun handle(address: String): BluetoothDevice? = handles[address]

    fun start(): Boolean {
        if (running) return true
        val sc = ctx.getSystemService(BluetoothManager::class.java)?.adapter?.takeIf { it.isEnabled }?.bluetoothLeScanner ?: return false
        return try {
            val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(BleProto.SERVICE)).build()
            val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
            sc.startScan(listOf(filter), settings, cb)
            running = true
            true
        } catch (_: Exception) { false }
    }

    fun stop() {
        if (!running) return
        runCatching { ctx.getSystemService(BluetoothManager::class.java)?.adapter?.bluetoothLeScanner?.stopScan(cb) }
        running = false
        found.clear(); handles.clear(); devices.value = emptyList()
    }
}
