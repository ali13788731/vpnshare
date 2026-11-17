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

enum class ConnError { WRONG_CODE, UNREACHABLE, ENGINE, VPN_DENIED, LOST, BAD_QR, DENIED, BLE, WIFI, CANCELED }

enum class Stage { REQUEST, WIFI, VERIFY, TUNNEL }

sealed class ConnFlow {
    data object Idle : ConnFlow()
    data class Working(val stage: Stage, val host: String?) : ConnFlow()
    data class NeedWifi(val ssid: String?, val retry: Boolean, val host: String?) : ConnFlow()
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

    private var app: Context? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private var watcher: Job? = null
    private var wifiCb: ConnectivityManager.NetworkCallback? = null
    private var wifiAnswer: CompletableDeferred<Pair<String, String>?>? = null

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

    fun submitWifi(ssid: String, psk: String) { wifiAnswer?.complete(ssid to psk) }
    fun cancelWifi() { wifiAnswer?.complete(null) }

    fun cancel() {
        job?.cancel()
        wifiAnswer?.complete(null)
        releaseNetwork()
        ClientVpnService.stop(ctx)
        flow.value = ConnFlow.Idle
    }

    fun disconnect() = cancel()

    fun reset() { if (flow.value is ConnFlow.Failed) flow.value = ConnFlow.Idle }

    private fun begin(host: String?, block: suspend () -> Unit) {
        job?.cancel(); watcher?.cancel()
        wifiAnswer?.complete(null)
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

    private suspend fun pipeline(offer0: Offer) {
        val host = offer0.name
        var offer = offer0
        flow.value = ConnFlow.Working(Stage.WIFI, host)

        var ssid = offer.ssid
        var psk = offer.psk
        var asked = false

        // 1) Wi-Fi
        if (ssid != null && psk != null) {
            if (!joinWifi(ssid, psk)) return fail(ConnError.WIFI)
        } else if (!Net.isWifiConnected(ctx)) {
            val ok = askAndJoin(ssid, host) ?: return fail(ConnError.CANCELED)
            if (!ok) return fail(ConnError.WIFI)
            asked = true
        } else {
            network = Net.wifiNetwork(ctx)
            network?.let { runCatching { cm.bindProcessToNetwork(it) } }
        }

        // 2) Verify the host
        flow.value = ConnFlow.Working(Stage.VERIFY, host)
        val port = Pairing.portFor(offer.code)
        var ip = resolveIp(offer)
        var res = ip?.let { probe(it, port, offer.code) } ?: Socks5Probe.Result.UNREACHABLE
        if (res == Socks5Probe.Result.UNREACHABLE && !asked && offer.ssid == null) {
            // Maybe we're on the wrong Wi-Fi: ask for the host hotspot credentials.
            val ok = askAndJoin(null, host) ?: return fail(ConnError.CANCELED)
            if (!ok) return fail(ConnError.WIFI)
            flow.value = ConnFlow.Working(Stage.VERIFY, host)
            ip = resolveIp(offer)
            res = ip?.let { probe(it, port, offer.code) } ?: Socks5Probe.Result.UNREACHABLE
        }
        when (res) {
            Socks5Probe.Result.WRONG_CODE -> return fail(ConnError.WRONG_CODE)
            Socks5Probe.Result.UNREACHABLE -> return fail(ConnError.UNREACHABLE)
            Socks5Probe.Result.OK -> {}
        }
        offer = offer.copy(ip = ip)

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

    private suspend fun probe(ip: String, port: Int, code: String) =
        withContext(Dispatchers.IO) { Socks5Probe.test(ip, port, code) }

    private fun resolveIp(o: Offer): String? =
        o.ip ?: network?.let { Net.gatewayOf(ctx, it) } ?: Net.gatewayIp(ctx)

    /** Asks the user (in-app) for the hotspot name & password, then joins. Null = user canceled. */
    private suspend fun askAndJoin(knownSsid: String?, host: String?): Boolean? {
        var retry = false
        repeat(3) {
            val d = CompletableDeferred<Pair<String, String>?>()
            wifiAnswer = d
            flow.value = ConnFlow.NeedWifi(knownSsid, retry, host)
            val a = d.await() ?: return null
            flow.value = ConnFlow.Working(Stage.WIFI, host)
            if (joinWifi(a.first, a.second)) return true
            retry = true
        }
        return false
    }

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
