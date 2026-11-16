package ir.mrpc.hotshare.client

import java.io.DataInputStream
import java.net.InetSocketAddress
import java.net.Socket

/** تست سریع اتصال و رمز قبل از ساخت VPN (و نگهبان دوره‌ای در حین اتصال). */
object Socks5Probe {
    enum class Result { OK, WRONG_CODE, UNREACHABLE }

    fun test(ip: String, port: Int, code: String): Result = try {
        Socket().use { s ->
            s.connect(InetSocketAddress(ip, port), 4000)
            s.soTimeout = 4000
            val o = s.getOutputStream()
            val i = DataInputStream(s.getInputStream())
            o.write(byteArrayOf(5, 1, 2)); o.flush()
            if (i.readUnsignedByte() != 5 || i.readUnsignedByte() != 2) {
                Result.UNREACHABLE
            } else {
                val user = "hotshare".toByteArray()
                val pass = code.toByteArray()
                o.write(byteArrayOf(1, user.size.toByte()) + user + byteArrayOf(pass.size.toByte()) + pass); o.flush()
                i.readUnsignedByte()
                if (i.readUnsignedByte() == 0) Result.OK else Result.WRONG_CODE
            }
        }
    } catch (_: Exception) {
        Result.UNREACHABLE
    }
}
