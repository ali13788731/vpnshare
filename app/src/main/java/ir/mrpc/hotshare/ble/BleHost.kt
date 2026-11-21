package ir.mrpc.hotshare.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Makes this phone discoverable (BLE advertising) and answers connection requests over GATT.
 * The receiver writes its name; once [ask] approves, the receiver can read the connection offer.
 */
@SuppressLint("MissingPermission")
class BleHost(
    private val ctx: Context,
    private val scope: CoroutineScope,
    private val name: () -> String,
    private val offer: () -> String,
    private val ask: suspend (String) -> Boolean
) {
    private var gatt: BluetoothGattServer? = null
    private var advertising = false
    private val status = ConcurrentHashMap<String, ByteArray>()

    val isAdvertising get() = advertising

    private val advCb = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) { advertising = true }
        override fun onStartFailure(errorCode: Int) { advertising = false }
    }

    private val serverCb = object : BluetoothGattServerCallback() {
        override fun onServiceAdded(st: Int, service: BluetoothGattService?) { startAdvertising() }

        override fun onConnectionStateChange(device: BluetoothDevice, st: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_DISCONNECTED) status.remove(device.address)
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice, requestId: Int, c: BluetoothGattCharacteristic,
            preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?
        ) {
            if (responseNeeded) gatt?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            if (c.uuid != BleProto.CHAR_REQ) return
            val addr = device.address
            val who = value?.toString(Charsets.UTF_8)?.trim().orEmpty().ifEmpty { "Device" }
            status[addr] = byteArrayOf(BleProto.ST_WAIT.toByte())
            scope.launch {
                val ok = runCatching { ask(who) }.getOrDefault(false)
                status[addr] = if (ok) byteArrayOf(BleProto.ST_OK.toByte()) + offer().toByteArray(Charsets.UTF_8)
                else byteArrayOf(BleProto.ST_DENIED.toByte())
            }
        }

        override fun onCharacteristicReadRequest(device: BluetoothDevice, requestId: Int, offset: Int, c: BluetoothGattCharacteristic) {
            val v = status[device.address] ?: byteArrayOf(BleProto.ST_WAIT.toByte())
            val out = if (offset >= v.size) ByteArray(0) else v.copyOfRange(offset, v.size)
            gatt?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, out)
        }
    }

    fun start(): Boolean {
        val bm = ctx.getSystemService(BluetoothManager::class.java) ?: return false
        val adapter = bm.adapter ?: return false
        if (!adapter.isEnabled || adapter.bluetoothLeAdvertiser == null) return false
        return try {
            val server = bm.openGattServer(ctx, serverCb) ?: return false
            gatt = server
            val svc = BluetoothGattService(BleProto.SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY)
            svc.addCharacteristic(
                BluetoothGattCharacteristic(BleProto.CHAR_REQ, BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE)
            )
            svc.addCharacteristic(
                BluetoothGattCharacteristic(BleProto.CHAR_RES, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ)
            )
            server.addService(svc)
            true
        } catch (_: Exception) { false }
    }

    private fun startAdvertising() {
        runCatching {
            val adv = ctx.getSystemService(BluetoothManager::class.java)?.adapter?.bluetoothLeAdvertiser ?: return
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .setTimeout(0)
                .build()
            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(ParcelUuid(BleProto.SERVICE))
                .build()
            val scanResp = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addManufacturerData(BleProto.COMPANY_ID, BleProto.clip(name(), 22))
                .build()
            adv.startAdvertising(settings, data, scanResp, advCb)
        }
    }

    fun stop() {
        runCatching { ctx.getSystemService(BluetoothManager::class.java)?.adapter?.bluetoothLeAdvertiser?.stopAdvertising(advCb) }
        runCatching { gatt?.clearServices(); gatt?.close() }
        gatt = null; advertising = false; status.clear()
    }
}
