package ir.mrpc.hotshare.client

import android.content.Intent
import android.net.ConnectivityManager
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import hev.htproxy.TProxyService
import ir.mrpc.hotshare.core.Net
import ir.mrpc.hotshare.core.Pairing
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

enum class ClientError { WRONG_CODE, UNREACHABLE, NATIVE, VPN_DENIED, LOST, BAD_QR, NO_WIFI }

sealed class ClientState {
    data object Idle : ClientState()
    data object Connecting : ClientState()
    data class Connected(val since: Long) : ClientState()
    data class Error(val kind: ClientError) : ClientState()
}

class ClientVpnService : VpnService() {
    companion object {
        val state = MutableStateFlow<ClientState>(ClientState.Idle)
        val stats = MutableStateFlow(0L to 0L)           // (ارسال، دریافت)
        const val EXTRA_IP = "ip"
        const val EXTRA_CODE = "code"
        const val ACTION_STOP = "ir.mrpc.hotshare.CLIENT_STOP"
    }

    private var tun: ParcelFileDescriptor? = null
    private var worker: Thread? = null
    @Volatile private var running = false
    private var nativeStarted = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            worker?.interrupt()
            cleanup(); state.value = ClientState.Idle
            stopSelf(); return START_NOT_STICKY
        }
        val ip = intent?.getStringExtra(EXTRA_IP)
        val code = intent?.getStringExtra(EXTRA_CODE)
        if (ip == null || code == null) { stopSelf(); return START_NOT_STICKY }
        worker?.interrupt()
        worker = Thread { connect(ip, code) }.also { it.start() }
        return START_NOT_STICKY
    }

    private fun connect(ip: String, code: String) {
        cleanup()
        state.value = ClientState.Connecting
        stats.value = 0L to 0L
        val cm = getSystemService(ConnectivityManager::class.java)
        val wifi = Net.wifiNetwork(this)
        wifi?.let { cm.bindProcessToNetwork(it) }

        val port = Pairing.portFor(code)
        when (Socks5Probe.test(ip, port, code)) {
            Socks5Probe.Result.WRONG_CODE -> return fail(ClientError.WRONG_CODE)
            Socks5Probe.Result.UNREACHABLE -> return fail(ClientError.UNREACHABLE)
            Socks5Probe.Result.OK -> {}
        }

        val b = Builder()
            .setSession("HotShare")
            .setMtu(1500)
            .addAddress("198.18.0.1", 32)
            .addAddress("fd00::1", 128)
            .addRoute("0.0.0.0", 0)
            .addRoute("::", 0)
            .addDnsServer("198.18.0.2")
        runCatching { b.addDisallowedApplication(packageName) }
        if (Build.VERSION.SDK_INT >= 29) b.setMetered(false)
        if (wifi != null) runCatching { b.setUnderlyingNetworks(arrayOf(wifi)) }
        val pfd = try { b.establish() } catch (_: Exception) { null } ?: return fail(ClientError.VPN_DENIED)
        tun = pfd

        val cfg = File(filesDir, "hev.yml")
        cfg.writeText(
            """
            tunnel:
              mtu: 1500
              ipv4: 198.18.0.1
              ipv6: 'fd00::1'
            socks5:
              port: $port
              address: $ip
              udp: 'udp'
              username: 'hotshare'
              password: '$code'
            mapdns:
              address: 198.18.0.2
              port: 53
              network: 100.64.0.0
              netmask: 255.192.0.0
              cache-size: 10000
            misc:
              task-stack-size: 81920
              connect-timeout: 5000
              read-write-timeout: 60000
              log-file: stderr
              log-level: warn
            """.trimIndent()
        )
        try {
            TProxyService.TProxyStartService(cfg.absolutePath, pfd.fd)
            nativeStarted = true
        } catch (_: Throwable) {
            return fail(ClientError.NATIVE)
        }

        state.value = ClientState.Connected(System.currentTimeMillis())
        running = true
        var misses = 0
        var tick = 0
        try {
            while (running) {
                Thread.sleep(1000)
                val st = runCatching { TProxyService.TProxyGetStats() }.getOrNull()
                if (st != null && st.size >= 4) stats.value = st[1] to st[3]
                if (++tick % 10 == 0) {
                    if (Socks5Probe.test(ip, port, code) == Socks5Probe.Result.OK) misses = 0
                    else if (++misses >= 3) return fail(ClientError.LOST)
                }
            }
        } catch (_: InterruptedException) { /* قطع اتصال توسط کاربر */ }
    }

    private fun fail(kind: ClientError) {
        cleanup()
        state.value = ClientState.Error(kind)
        stopSelf()
    }

    private fun cleanup() {
        running = false
        if (nativeStarted) { runCatching { TProxyService.TProxyStopService() }; nativeStarted = false }
        runCatching { tun?.close() }; tun = null
        runCatching { getSystemService(ConnectivityManager::class.java).bindProcessToNetwork(null) }
    }

    override fun onRevoke() {
        worker?.interrupt(); cleanup(); state.value = ClientState.Idle; super.onRevoke()
    }

    override fun onDestroy() {
        worker?.interrupt(); cleanup()
        if (state.value is ClientState.Connected || state.value is ClientState.Connecting) state.value = ClientState.Idle
        super.onDestroy()
    }
}
