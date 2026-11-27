package ir.mrpc.hotshare.core

import android.net.Uri
import java.security.SecureRandom

/** Everything a receiver needs to reach a host. [ip] is null when only the code is known. */
data class Offer(
    val ip: String?,
    val code: String,
    val ssid: String? = null,
    val psk: String? = null,
    val name: String? = null
)

object Pairing {
    private val rnd = SecureRandom()

    /** Random 8-digit code. It is the SOCKS5 password and the listening port is derived from it. */
    fun newCode(): String = (1..8).joinToString("") { rnd.nextInt(10).toString() }

    fun portFor(code: String): Int = 20000 + (code.take(4).toIntOrNull() ?: 0) % 20000

    fun pretty(code: String) = code.chunked(4).joinToString("  ")

    fun toUri(o: Offer): String {
        val b = Uri.Builder().scheme("hotshare").authority("connect")
        o.ip?.let { b.appendQueryParameter("ip", it) }
        b.appendQueryParameter("code", o.code)
        o.ssid?.takeIf { it.isNotEmpty() }?.let { b.appendQueryParameter("ssid", it) }
        o.psk?.takeIf { it.isNotEmpty() }?.let { b.appendQueryParameter("psk", it) }
        o.name?.takeIf { it.isNotEmpty() }?.let { b.appendQueryParameter("n", it) }
        return b.build().toString()
    }

    fun parse(text: String): Offer? {
        val u = runCatching { Uri.parse(text.trim()) }.getOrNull() ?: return null
        if (u.scheme != "hotshare") return null
        val code = u.getQueryParameter("code") ?: return null
        if (!code.matches(Regex("\\d{8}"))) return null
        val ip = u.getQueryParameter("ip")
        if (ip != null && !ip.matches(Regex("\\d{1,3}(\\.\\d{1,3}){3}"))) return null
        return Offer(ip, code, u.getQueryParameter("ssid"), u.getQueryParameter("psk"), u.getQueryParameter("n"))
    }

    private const val B64 = android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING

    /** SOCKS5 username that carries this phone's name so the host can show who is connected. */
    fun userFor(name: String): String =
        "hs:" + android.util.Base64.encodeToString(name.trim().take(24).ifEmpty { "Device" }.toByteArray(Charsets.UTF_8), B64)

    fun nameFromUser(u: String): String? =
        if (!u.startsWith("hs:")) null
        else runCatching { String(android.util.Base64.decode(u.substring(3), B64), Charsets.UTF_8) }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }

    /** Persian/Arabic digits to ASCII. */
    fun normalizeDigits(s: String) = s.map {
        when (it) {
            in '۰'..'۹' -> '0' + (it - '۰')
            in '٠'..'٩' -> '0' + (it - '٠')
            else -> it
        }
    }.joinToString("")
}
