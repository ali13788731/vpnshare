package ir.mrpc.hotshare.client

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import ir.mrpc.hotshare.ble.BleClient
import ir.mrpc.hotshare.ble.BleResult
import ir.mrpc.hotshare.ble.NearbyDevice
import ir.mrpc.hotshare.core.Net
import ir.mrpc.hotshare.core.Offer
import ir.mrpc.hotshare.core.Pairing
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

enum class ConnError {
    WRONG_CODE, UNREACHABLE, ENGINE, VPN_DENIED, BAD_QR, DENIED, BLE, CANCELED,
    /** The host's Wi-Fi password is wrong / was changed, or the hotspot is out of range. */
    WIFI_PASSWORD,
    /** The phone left the host's Wi-Fi while connected (hotspot off, out of range, password changed). */
    WIFI_LOST,
    /** Wi-Fi is fine but the host stopped sharing. */
    HOST_STOPPED,
    /** Kept for old callers: connection dropped for an unknown reason. */
    LOST,
    WIFI
}

enum class Stage { REQUEST, WIFI, VERIFY, TUNNEL }

/** Why the "join the host's Wi-Fi" guide is shown. */
enum class WifiReason {
    NOT_CONNECTED,   // phone is on no Wi-Fi
    WRONG_NETWORK,   // phone is on another Wi-Fi than the host's hotspot
    JOIN_FAILED,     // automatic joining failed (wrong / changed password, out of range)
    NOT_HOST         // joined a network, but the host isn't sharing on it
}

sealed class ConnFlow {
    data object Idle : ConnFlow()
    data class Working(val stage: Stage, val host: String?) : ConnFlow()
    /**
     * The phone is not on the host's Wi-Fi yet. The app does NOT scan or list networks: it names the
     * hotspot ([expected], when known), opens the phone's own Wi-Fi settings on request and keeps checking
     * by itself until the host is reachable.
     */
    data class NeedWifi(
        val expected: String?,
        val psk: String?,
        val reason: WifiReason,
        val current: String?,
        val host: String?
    ) : ConnFlow() {
        val hasKey get() = !psk.isNullOrEmpty()
    }
    data class Failed(val error: ConnError) : ConnFlow()
    data class Done(val host: String?) : ConnFlow()
}

/**
 * Orchestrates the receiver side: ask host (BLE) -> join Wi-Fi -> verify -> start VPN tunnel.
 * The VPN permission must already have been granted by the caller (needs an Activity).
 */
object Connector {
    val flow = MutableStateFlow<ConnFlow>(ConnFlow.Idle)

    @Volatile var network: Network? = null
        private set

    /** The last offer we tried, so "Try again" can repeat it without scanning / typing again. */
    @Volatile var lastOffer: Offer? = null
        private set

    private var app: Context? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private var watcher: Job? = null
    private var wifiCb: ConnectivityManager.NetworkCallback? = null
    @Volatile private var retryJoin = false

    @Volatile var viaDevice = false

    fun init(ctx: Context) { app = ctx.applicationContext }

    /** Called right before the VPN permission prompt so the UI can already show progress. */
    fun markStarting(host: String?, via: Boolean) {
        viaDevice = via
        flow.value = ConnFlow.Working(if (via) Stage.REQUEST else Stage.WIFI, host)
    }

    fun failExternal(e: ConnError) = fail(e)

    private val ctx get() = app!!
    private val cm get() = ctx.getSystemService(ConnectivityManager::class.java)

    fun connectDevice(d: NearbyDevice, handle: android.bluetooth.BluetoothDevice) = begin(d.name) {
        flow.value = ConnFlow.Working(Stage.REQUEST, d.name)
        when (val r = BleClient.requestOffer(ctx, handle, Net.deviceName(ctx))) {
            is BleResult.Offer -> Pairing.parse(r.uri)?.copy(name = d.name) ?: return@begin fail(ConnError.BAD_QR)
            BleResult.Denied -> return@begin fail(ConnError.DENIED)
            BleResult.Failed -> return@begin fail(ConnError.BLE)
        }.let { pipeline(it) }
    }

    fun connectOffer(o: Offer) = begin(o.name) { pipeline(o) }

    fun connectCode(code: String) = begin(null) { pipeline(Offer(null, code)) }

    /** The user pressed "try joining again" in the Wi-Fi guide (after fixing the hotspot / password). */
    fun retryWifiJoin() { retryJoin = true }

    /** Cancels whatever is running (also while waiting for the user to join Wi-Fi) and disconnects. */
    fun cancel() {
        job?.cancel()
        watcher?.cancel()
        releaseNetwork()
        ClientVpnService.stop(ctx)
        flow.value = ConnFlow.Idle
    }

    fun disconnect() = cancel()

    /** A receive session is running or connecting (so this phone can't start sharing at the same time). */
    fun isBusy(): Boolean = flow.value is ConnFlow.Working || flow.value is ConnFlow.NeedWifi || flow.value is ConnFlow.Done

    fun reset() { if (flow.value is ConnFlow.Failed) flow.value = ConnFlow.Idle }

    private fun begin(host: String?, block: suspend () -> Unit) {
        job?.cancel(); watcher?.cancel()
        retryJoin = false
        flow.value = ConnFlow.Working(Stage.WIFI, host)
        job = scope.launch {
            try { block() } catch (_: CancellationException) { } catch (_: Throwable) { fail(ConnError.UNREACHABLE) }
        }
    }

    private fun fail(e: ConnError) {
        releaseNetwork()
        flow.value = ConnFlow.Failed(e)
    }

    private fun releaseNetwork() {
        wifiCb?.let { runCatching { cm.unregisterNetworkCallback(it) } }
        wifiCb = null
        network = null
        runCatching { cm.bindProcessToNetwork(null) }
    }

    private fun bindToWifi(): Boolean {
        val n = Net.wifiNetwork(ctx) ?: return false
        network = n
        runCatching { cm.bindProcessToNetwork(n) }
        return true
    }

    private suspend fun pipeline(offer: Offer) {
        lastOffer = offer
        val host = offer.name
        val port = Pairing.portFor(offer.code)
        val user = Pairing.userFor(Net.deviceName(ctx))
        val expected = offer.ssid?.takeIf { it.isNotBlank() }
        val hasKey = expected != null && !offer.psk.isNullOrEmpty()
        flow.value = ConnFlow.Working(Stage.WIFI, host)
        releaseNetwork()

        var ip: String? = null
        var res = Socks5Probe.Result.UNREACHABLE
        var reason = WifiReason.NOT_CONNECTED
        var autoTried = false

        /** Is the host really reachable on the Wi-Fi the phone is on now? (checked by reaching it, not only by name) */
        suspend fun tryCurrentWifi(tries: Int, stage: Boolean): Boolean {
            if (!Net.isWifiConnected(ctx)) return false
            val cur = Net.currentSsid(ctx)
            if (expected != null && cur != null && cur != expected) { reason = WifiReason.WRONG_NETWORK; return false }
            if (!bindToWifi()) return false
            if (stage) flow.value = ConnFlow.Working(Stage.VERIFY, host)
            ip = resolveIp(offer)
            res = ip?.let { probeRetry(it, port, offer.code, user, tries) } ?: Socks5Probe.Result.UNREACHABLE
            if (res == Socks5Probe.Result.UNREACHABLE) { releaseNetwork(); reason = WifiReason.NOT_HOST; return false }
            return true
        }

        // 1) already on the host's Wi-Fi?
        if (!tryCurrentWifi(3, true) && !Net.isWifiConnected(ctx)) reason = WifiReason.NOT_CONNECTED

        // 2) not yet: join automatically when we know name + password, otherwise guide the user and wait
        while (res == Socks5Probe.Result.UNREACHABLE) {
            if (hasKey && !autoTried) {
                autoTried = true
                flow.value = ConnFlow.Working(Stage.WIFI, host)
                if (joinWifi(expected!!, offer.psk.orEmpty())) {
                    if (tryCurrentWifiAfterJoin(offer, port, user, host)) { ip = resolveIp(offer); res = Socks5Probe.Result.OK; break }
                    reason = WifiReason.NOT_HOST; releaseNetwork()
                } else reason = WifiReason.JOIN_FAILED
            }
            // wait here (the UI shows the hotspot name + a button to the phone's Wi-Fi settings); keep checking by ourselves
            var need = ConnFlow.NeedWifi(expected, offer.psk?.takeIf { it.isNotEmpty() }, reason, Net.currentSsid(ctx), host)
            flow.value = need
            var waited = 0
            while (res == Socks5Probe.Result.UNREACHABLE) {
                delay(1500)
                if (retryJoin) { retryJoin = false; autoTried = false; break }
                val cur = if (Net.isWifiConnected(ctx)) Net.currentSsid(ctx) else null
                if (cur != need.current) { need = need.copy(current = cur); flow.value = need }
                if (Net.isWifiConnected(ctx) && (expected == null || cur == null || cur == expected)) {
                    waited++
                    // one quick check per tick; a failed check is quiet (the guide stays on screen)
                    val savedReason = reason
                    if (tryCurrentWifi(1, false)) break
                    reason = savedReason
                    if (need.reason != reason) { need = need.copy(reason = reason); flow.value = need }
                }
            }
        }
        when (res) {
            Socks5Probe.Result.WRONG_CODE -> return fail(ConnError.WRONG_CODE)
            Socks5Probe.Result.UNREACHABLE -> return fail(ConnError.UNREACHABLE)
            Socks5Probe.Result.OK -> {}
        }

        // 3) Tunnel
        flow.value = ConnFlow.Working(Stage.TUNNEL, host)
        ClientVpnService.state.value = ClientState.Connecting
        ClientVpnService.start(ctx, ip!!, offer.code)
        val end = withTimeoutOrNull(20_000) {
            ClientVpnService.state.first { it is ClientState.Connected || it is ClientState.Error }
        }
        when (end) {
            is ClientState.Connected -> {
                flow.value = ConnFlow.Done(host)
                watch()
            }
            is ClientState.Error -> fail(end.kind)
            else -> { ClientVpnService.stop(ctx); fail(ConnError.ENGINE) }
        }
    }

    /** After an automatic join the network is already bound; just verify the host. */
    private suspend fun tryCurrentWifiAfterJoin(offer: Offer, port: Int, user: String, host: String?): Boolean {
        flow.value = ConnFlow.Working(Stage.VERIFY, host)
        val ip = resolveIp(offer) ?: return false
        return probeRetry(ip, port, offer.code, user, 3) != Socks5Probe.Result.UNREACHABLE
    }

    private fun watch() {
        watcher?.cancel()
        watcher = scope.launch {
            ClientVpnService.state.collect { s ->
                when (s) {
                    is ClientState.Error -> { releaseNetwork(); flow.value = ConnFlow.Failed(s.kind); watcher?.cancel() }
                    ClientState.Idle -> { if (flow.value is ConnFlow.Done) { releaseNetwork(); flow.value = ConnFlow.Idle }; watcher?.cancel() }
                    else -> {}
                }
            }
        }
    }

    private suspend fun probe(ip: String, port: Int, code: String, user: String) =
        withContext(Dispatchers.IO) { Socks5Probe.test(ip, port, code, user, 2500) }

    /**
     * Right after joining, DHCP / the host's proxy may need a moment. A single failed probe used to send the
     * user back to the password page even though Wi-Fi was connected, so retry a few times first.
     */
    private suspend fun probeRetry(ip: String, port: Int, code: String, user: String, tries: Int = 3): Socks5Probe.Result {
        var r = Socks5Probe.Result.UNREACHABLE
        repeat(tries) { i ->
            r = probe(ip, port, code, user)
            if (r != Socks5Probe.Result.UNREACHABLE) return r
            if (i < tries - 1) delay(800)
        }
        return r
    }

    private fun resolveIp(o: Offer): String? =
        o.ip ?: network?.let { Net.gatewayOf(ctx, it) } ?: Net.gatewayIp(ctx)

    private suspend fun joinWifi(ssid: String, psk: String): Boolean {
        releaseNetwork()
        return if (Build.VERSION.SDK_INT >= 29) joinSpecifier(ssid, psk) else joinLegacy(ssid, psk)
    }

    private suspend fun joinSpecifier(ssid: String, psk: String): Boolean {
        val n = withTimeoutOrNull(40_000) {
            suspendCancellableCoroutine<Network?> { cont ->
                val spec = WifiNetworkSpecifier.Builder().setSsid(ssid)
                    .apply { if (psk.length >= 8) setWpa2Passphrase(psk) }.build()
                val req = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .setNetworkSpecifier(spec)
                    .build()
                val cb = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(n: Network) { if (cont.isActive) cont.resumeWith(Result.success(n)) }
                    override fun onUnavailable() { if (cont.isActive) cont.resumeWith(Result.success(null)) }
                }
                wifiCb = cb
                cm.requestNetwork(req, cb)
                cont.invokeOnCancellation { runCatching { cm.unregisterNetworkCallback(cb) } }
            }
        }
        if (n == null) { releaseNetwork(); return false }
        // wait for DHCP
        var waited = 0
        while (waited < 25 && !Net.hasIpv4(ctx, n)) { delay(200); waited++ }
        network = n
        runCatching { cm.bindProcessToNetwork(n) }
        return true
    }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private suspend fun joinLegacy(ssid: String, psk: String): Boolean {
        val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return try {
            val conf = WifiConfiguration().apply {
                SSID = "\"$ssid\""
                if (psk.isEmpty()) allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE) else preSharedKey = "\"$psk\""
            }
            if (!wm.isWifiEnabled) wm.isWifiEnabled = true
            val id = wm.addNetwork(conf)
            if (id < 0) return false
            wm.disconnect(); wm.enableNetwork(id, true); wm.reconnect()
            val ok = withTimeoutOrNull(30_000) {
                while (true) {
                    val cur = wm.connectionInfo?.ssid?.trim('"')
                    if (cur == ssid && Net.gatewayIp(ctx) != null && Net.isWifiConnected(ctx)) break
                    delay(500)
                }
                true
            } ?: false
            if (ok) { network = Net.wifiNetwork(ctx); network?.let { runCatching { cm.bindProcessToNetwork(it) } } }
            ok
        } catch (_: Exception) { false }
    }
}
