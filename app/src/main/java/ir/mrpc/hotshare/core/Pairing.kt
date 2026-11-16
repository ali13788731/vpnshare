package ir.mrpc.hotshare.core

import android.net.Uri
import java.security.SecureRandom

object Pairing {
    private val rnd = SecureRandom()

    data class Link(val ip: String, val code: String)

    /** کد ۸ رقمی تصادفی؛ هم رمز SOCKS5 است و هم پورت از روی آن ساخته می‌شود. */
    fun newCode(): String = (1..8).joinToString("") { rnd.nextInt(10).toString() }

    fun portFor(code: String): Int = 20000 + (code.take(4).toIntOrNull() ?: 0) % 20000

    fun pretty(code: String) = code.chunked(4).joinToString("  ")

    fun toUri(ip: String, code: String) = "hotshare://connect?ip=$ip&code=$code"

    fun parse(text: String): Link? {
        val u = runCatching { Uri.parse(text.trim()) }.getOrNull() ?: return null
        if (u.scheme != "hotshare") return null
        val ip = u.getQueryParameter("ip") ?: return null
        val code = u.getQueryParameter("code") ?: return null
        if (!code.matches(Regex("\\d{8}"))) return null
        if (!ip.matches(Regex("\\d{1,3}(\\.\\d{1,3}){3}"))) return null
        return Link(ip, code)
    }

    /** ارقام فارسی/عربی را به ASCII تبدیل می‌کند. */
    fun normalizeDigits(s: String) = s.map {
        when (it) {
            in '۰'..'۹' -> '0' + (it - '۰')
            in '٠'..'٩' -> '0' + (it - '٠')
            else -> it
        }
    }.joinToString("")
}
