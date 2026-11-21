package ir.mrpc.hotshare.client

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Network
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import hev.htproxy.TProxyService
import ir.mrpc.hotshare.MainActivity
import ir.mrpc.hotshare.core.Net
import ir.mrpc.hotshare.core.Pairing
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

sealed class ClientState {
    data object Idle : ClientState()
    data object Connecting : ClientState()
    data class Connected(val since: Long) : ClientState()
    data class Error(val kind: ConnError) : ClientState()
}

class ClientVpnService : VpnService() {
    companion object {
        val state = MutableStateFlow<ClientState>(ClientState.Idle)
        val up = MutableStateFlow(0L)
        val down = MutableStateFlow(0L)
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"

        fun start(ctx: Context, ip: String, code: String) {
            ctx.startService(
                Intent(ctx, ClientVpnService::class.java).setAction(ACTION_START)
                    .putExtra("ip", ip).putExtra("code", code)
            )
        }

        fun stop(ctx: Context) {
            ctx.startService(Intent(ctx, ClientVpnService::class.java).setAction(ACTION_STOP))
        }
    }

    private val lock = Any()
    private var tun: ParcelFileDescriptor? = null
    private var nativeStarted = false
    @Volatile private var running = false
    private var worker: Thread? = null
    private var wake: PowerManager.WakeLock? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                Thread {
                    teardown()
                    state.value = ClientState.Idle
                    stopSelf()
                }.start()
            }
            ACTION_START -> {
                val ip = intent.getStringExtra("ip") ?: return START_NOT_STICKY
                val code = intent.getStringExtra("code") ?: return START_NOT_STICKY
                state.value = ClientState.Connecting
                worker?.interrupt()
                worker = Thread { connect(ip, code) }.also { it.isDaemon = true; it.start() }
            }
        }
        return START_NOT_STICKY
    }

    private fun fail(kind: ConnError) {
        teardown()
        state.value = ClientState.Error(kind)
        stopSelf()
    }

    private fun connect(ip: String, code: String) {
        val port = Pairing.portFor(code)
        val network: Network? = Connector.network ?: Net.wifiNetwork(this)
        try {
            val cfgFile = File(cacheDir, "tproxy.yml")
            cfgFile.writeText(
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
                  tcp-read-write-timeout: 300000
                  udp-read-write-timeout: 60000
                  log-file: stderr
                  log-level: warn
                """.trimIndent()
            )

            val b = Builder()
                .setSession("HotShare")
                .setMtu(1500)
                .addAddress("198.18.0.1", 32)
                .addAddress("fd00::1", 128)
                .addRoute("0.0.0.0", 0)
                .addRoute("::", 0)
                .addDnsServer("198.18.0.2")
                .setConfigureIntent(
                    PendingIntent.getActivity(
                        this, 0, Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                )
            // The tunnel's own sockets (to the host) must bypass the VPN.
            runCatching { b.addDisallowedApplication(packageName) }
            if (network != null) b.setUnderlyingNetworks(arrayOf(network))

            val pfd = b.establish()
            if (pfd == null) { fail(ConnError.VPN_DENIED); return }
            synchronized(lock) { tun = pfd }

            network?.let { runCatching { getSystemService(android.net.ConnectivityManager::class.java).bindProcessToNetwork(it) } }

            val started = synchronized(lock) {
                try {
                    TProxyService.TProxyStartService(cfgFile.absolutePath, pfd.fd).also { nativeStarted = it }
                } catch (_: Throwable) { false }
            }
            if (!started) { fail(ConnError.ENGINE); return }

            Thread.sleep(700)
            val alive = try { TProxyService.TProxyIsRunning() } catch (_: Throwable) { false }
            if (!alive) { fail(ConnError.ENGINE); return }

            running = true
            wake = (getSystemService(Context.POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "hotshare:client")
                .apply { setReferenceCounted(false); acquire(6 * 60 * 60 * 1000L) }
            up.value = 0; down.value = 0
            state.value = ClientState.Connected(System.currentTimeMillis())

            var misses = 0
            while (running) {
                Thread.sleep(2000)
                if (!running) break
                val st = synchronized(lock) {
                    if (nativeStarted) try { TProxyService.TProxyGetStats() } catch (_: Throwable) { null } else null
                }
                if (st != null && st.size >= 4) { up.value = st[1]; down.value = st[3] }
                if (Socks5Probe.test(ip, port, code) == Socks5Probe.Result.UNREACHABLE) misses++ else misses = 0
                if (misses >= 3) { fail(ConnError.LOST); return }
            }
        } catch (_: InterruptedException) {
            // replaced by a newer start
        } catch (_: Throwable) {
            fail(ConnError.ENGINE)
        }
    }

    private fun teardown() {
        running = false
        synchronized(lock) {
            if (nativeStarted) {
                runCatching { TProxyService.TProxyStopService() }
                nativeStarted = false
            }
            runCatching { tun?.close() }
            tun = null
        }
        wake?.let { if (it.isHeld) it.release() }; wake = null
        runCatching { getSystemService(android.net.ConnectivityManager::class.java).bindProcessToNetwork(Connector.network) }
    }

    override fun onRevoke() {
        Thread {
            teardown()
            state.value = ClientState.Idle
            stopSelf()
        }.start()
    }

    override fun onDestroy() {
        teardown()
        if (state.value is ClientState.Connected || state.value is ClientState.Connecting) state.value = ClientState.Idle
        super.onDestroy()
    }
}
