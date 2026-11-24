package ir.mrpc.hotshare.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import ir.mrpc.hotshare.core.Prefs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/** Asks a nearby host (over GATT) for permission and receives its connection offer. */
object BleClient {

    suspend fun requestOffer(ctx: Context, device: BluetoothDevice, myName: String): BleResult {
        repeat(2) { attempt ->
            val s = Session(ctx.applicationContext, device, myName)
            val r = withTimeoutOrNull(90_000) { s.run() } ?: BleResult.Failed
            s.close()
            if (r !is BleResult.Failed || attempt == 1) return r
            delay(700)
        }
        return BleResult.Failed
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private class Session(val ctx: Context, val device: BluetoothDevice, val myName: String) : BluetoothGattCallback() {
        private val result = CompletableDeferred<BleResult>()
        private val main = Handler(Looper.getMainLooper())
        private var gatt: BluetoothGatt? = null
        private var polls = 0
        @Volatile private var closed = false

        suspend fun run(): BleResult {
            gatt = try {
                device.connectGatt(ctx, false, this, BluetoothDevice.TRANSPORT_LE)
            } catch (_: Exception) { null }
            if (gatt == null) return BleResult.Failed
            return result.await()
        }

        fun close() {
            if (closed) return
            closed = true
            main.removeCallbacksAndMessages(null)
            runCatching { gatt?.disconnect() }
            runCatching { gatt?.close() }
            gatt = null
        }

        private fun finish(r: BleResult) { if (!result.isCompleted) result.complete(r) }

        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                main.postDelayed({ if (!closed) runCatching { g.discoverServices() } }, 300)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED || status != BluetoothGatt.GATT_SUCCESS) {
                finish(BleResult.Failed)
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS || g.getService(BleProto.SERVICE) == null) { finish(BleResult.Failed); return }
            val asked = runCatching { g.requestMtu(247) }.getOrDefault(false)
            if (!asked) writeRequest(g)
        }

        override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) { writeRequest(g) }

        private fun writeRequest(g: BluetoothGatt) {
            val c = g.getService(BleProto.SERVICE)?.getCharacteristic(BleProto.CHAR_REQ)
            if (c == null) { finish(BleResult.Failed); return }
            c.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            c.value = BleProto.hello(Prefs.deviceId, myName)
            if (!g.writeCharacteristic(c)) finish(BleResult.Failed)
        }

        override fun onCharacteristicWrite(g: BluetoothGatt, c: BluetoothGattCharacteristic, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) { finish(BleResult.Failed); return }
            poll(g)
        }

        private fun poll(g: BluetoothGatt) {
            if (closed) return
            val c = g.getService(BleProto.SERVICE)?.getCharacteristic(BleProto.CHAR_RES)
            if (c == null || !g.readCharacteristic(c)) finish(BleResult.Failed)
        }

        override fun onCharacteristicRead(g: BluetoothGatt, c: BluetoothGattCharacteristic, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) { finish(BleResult.Failed); return }
            val v = c.value ?: ByteArray(0)
            when (v.firstOrNull()?.toInt()) {
                BleProto.ST_OK -> finish(BleResult.Offer(String(v, 1, v.size - 1, Charsets.UTF_8)))
                BleProto.ST_DENIED -> finish(BleResult.Denied)
                else -> {
                    if (++polls > 110) finish(BleResult.Denied)
                    else main.postDelayed({ poll(g) }, 600)
                }
            }
        }
    }
}
