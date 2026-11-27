package ir.mrpc.hotshare.ble

import java.util.UUID

object BleProto {
    val SERVICE: UUID = UUID.fromString("5f1c0001-8a3e-4d2b-9c7a-6d6f74736872")
    val CHAR_REQ: UUID = UUID.fromString("5f1c0002-8a3e-4d2b-9c7a-6d6f74736872")   // client writes its name
    val CHAR_RES: UUID = UUID.fromString("5f1c0003-8a3e-4d2b-9c7a-6d6f74736872")   // client reads status / offer
    const val COMPANY_ID = 0x0FF0

    const val ST_WAIT = 0
    const val ST_OK = 1
    const val ST_DENIED = 2

    /** Client hello: "<12 hex id>|<name>" */
    fun hello(id: String, name: String): ByteArray = "$id|".toByteArray(Charsets.UTF_8) + clip(name, 40)

    fun parseHello(raw: String): Pair<String?, String> {
        val i = raw.indexOf('|')
        if (i == 12 && raw.substring(0, 12).all { it in '0'..'9' || it in 'a'..'f' })
            return raw.substring(0, 12) to raw.substring(13).trim().ifEmpty { "Device" }
        return null to raw.ifEmpty { "Device" }
    }

    /** UTF-8 prefix of at most [maxBytes] bytes, cut on a character boundary. */
    fun clip(s: String, maxBytes: Int): ByteArray {
        val sb = StringBuilder()
        var size = 0
        for (ch in s.trim()) {
            val n = ch.toString().toByteArray(Charsets.UTF_8).size
            if (size + n > maxBytes) break
            sb.append(ch); size += n
        }
        return sb.toString().toByteArray(Charsets.UTF_8)
    }
}

data class NearbyDevice(
    val address: String,
    val name: String,
    val rssi: Float,
    val lastSeen: Long
)

sealed class BleResult {
    data class Offer(val uri: String) : BleResult()
    data object Denied : BleResult()
    data object Failed : BleResult()
}
