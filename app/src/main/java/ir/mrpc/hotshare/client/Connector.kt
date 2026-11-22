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

/** Why the Wi-Fi picker is shown (drives the red message above the list). */
enum class WifiReason {
    NOT_CONNECTED,   // phone is on no Wi-Fi
    WRONG_NETWORK,   // phone is on another Wi-Fi than the host's hotspot
    WRONG_PICK,      // user tapped a network that is not the host's hotspot
    JOIN_FAILED,     // joining failed (wrong password / out of range)
    NOT_HOST         // joined, but the host phone isn't on that network
}

class WifiPick(val ssid: String, val psk: String)

sealed class ConnFlow {
    data object Idle : ConnFlow()
    data class Working(val stage: Stage, val host: String?) : ConnFlow()
    /**
     * The phone is not on the host's Wi-Fi yet: the UI shows the live list of nearby networks.
     * [expected] is the host's hotspot name when we know it (QR / radar / saved); then ONLY that network is accepted.
     */
    data class NeedWifi(
        val expected: String?,
        val hasKey: Boolean,
        val reason: WifiReason,
        val current: String?,
        val tried: String?,
        val host: String?
    ) : ConnFlow()
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
    private var wifiAnswer: CompletableDeferred<WifiPick?>? = null

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

    /** [psk] may be empty: the saved password of the host hotspot (or an open network) is then used. */
    fun submitWifi(ssid: String, psk: String) { wifiAnswer?.complete(WifiPick(ssid.trim(), psk)) }
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
        val offer = offer0
        val port = Pairing.portFor(offer.code)
        val expected = offer.ssid?.takeIf { it.isNotBlank() }
        val hasKey = expected != null && !offer.psk.isNullOrEmpty()
        flow.value = ConnFlow.Working(Stage.WIFI, host)
        releaseNetwork()

        var ip: String? = null
        var res = Socks5Probe.Result.UNREACHABLE
        var reason = WifiReason.NOT_CONNECTED
        var current: String? = null
        var tried: String? = null

        // 1) Is the phone already on the host's Wi-Fi? (checked by really reaching the host, not only by name)
        if (Net.isWifiConnected(ctx)) {
            current = Net.currentSsid(ctx)
            reason = WifiReason.WRONG_NETWORK
            if (expected == null || current == null || current == expected) {
                network = Net.wifiNetwork(ctx)
                network?.let { runCatching { cm.bindProcessToNetwork(it) } }
                flow.value = ConnFlow.Working(Stage.VERIFY, host)
                ip = resolveIp(offer)
                res = ip?.let { probe(it, port, offer.code) } ?: Socks5Probe.Result.UNREACHABLE
                if (res == Socks5Probe.Result.UNREACHABLE) releaseNetwork()
            }
        }

        // 2) Not on the host's network: show nearby Wi-Fi networks and do not go on until the host is really reachable
        while (res == Socks5Probe.Result.UNREACHABLE) {
            val pick = askWifi(ConnFlow.NeedWifi(expected, hasKey, reason, current, tried, host)) ?: return fail(ConnError.CANCELED)
            tried = pick.ssid
            if (expected != null && pick.ssid != expected) { reason = WifiReason.WRONG_PICK; continue }
            val psk = if (pick.psk.isEmpty() && pick.ssid == expected) offer.psk.orEmpty() else pick.psk
            flow.value = ConnFlow.Working(Stage.WIFI, host)
            if (!joinWifi(pick.ssid, psk)) { reason = WifiReason.JOIN_FAILED; continue }
            flow.value = ConnFlow.Working(Stage.VERIFY, host)
            current = pick.ssid
            ip = resolveIp(offer)
            res = ip?.let { probe(it, port, offer.code) } ?: Socks5Probe.Result.UNREACHABLE
            if (res == Socks5Probe.Result.UNREACHABLE) { releaseNetwork(); reason = WifiReason.NOT_HOST }
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

    private suspend fun askWifi(n: ConnFlow.NeedWifi): WifiPick? {
        val d = CompletableDeferred<WifiPick?>()
        wifiAnswer = d
        flow.value = n
        return d.await()
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
