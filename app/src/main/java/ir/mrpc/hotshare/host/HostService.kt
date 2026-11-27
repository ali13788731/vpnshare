package ir.mrpc.hotshare.host

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import ir.mrpc.hotshare.MainActivity
import ir.mrpc.hotshare.R
import ir.mrpc.hotshare.ble.BleHost
import ir.mrpc.hotshare.core.Net
import ir.mrpc.hotshare.core.Offer
import ir.mrpc.hotshare.core.Pairing
import ir.mrpc.hotshare.core.Perms
import ir.mrpc.hotshare.core.Prefs
import ir.mrpc.hotshare.core.Strings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

enum class ApMode { NONE, SYSTEM, LOCAL }

/** Exact state of "can nearby phones see me over Bluetooth?" so the UI never has to guess. */
enum class BleState {
    UNSUPPORTED,    // this phone cannot be discovered over Bluetooth LE (QR / code still work)
    NO_PERMISSION,  // Bluetooth permission not granted
    BT_OFF,         // Bluetooth is switched off
    STARTING,       // switched on, starting to advertise
    ON              // visible to nearby phones
}

data class HostInfo(
    val running: Boolean = false,
    val clients: Int = 0,
    val devices: List<ClientInfo> = emptyList(),
    val up: Long = 0,
    val down: Long = 0,
    val error: String? = null,
    val ip: String? = null,
    val ssid: String? = null,
    val psk: String? = null,
    val ap: ApMode = ApMode.NONE,
    val apFailed: Boolean = false,
    val ble: Boolean = false,
    val bleState: BleState = BleState.BT_OFF,
    val vpn: Boolean = false
)

enum class ApprovalChoice { DENY, ONCE, ALWAYS }

class ApprovalRequest(
    val name: String,
    val canRemember: Boolean,
    val result: CompletableDeferred<ApprovalChoice> = CompletableDeferred()
)

class HostService : Service() {
    companion object {
        val state = MutableStateFlow(HostInfo())
        val pending = MutableStateFlow<ApprovalRequest?>(null)
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
        private const val CH = "share"
        private const val CH_ASK = "ask"

        fun start(ctx: Context) {
            val i = Intent(ctx, HostService::class.java).setAction(ACTION_START)
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i) else ctx.startService(i)
        }

        fun stop(ctx: Context) {
            ctx.startService(Intent(ctx, HostService::class.java).setAction(ACTION_STOP))
        }

        /** Offer that is shown as QR code and handed to approved devices. */
        fun currentOffer(ctx: Context): Offer {
            val s = state.value
            return Offer(
                ip = s.ip ?: Net.hotspotIp(),
                code = Prefs.code,
                ssid = s.ssid?.takeIf { it.isNotEmpty() } ?: Prefs.apSsid.takeIf { it.isNotEmpty() },
                psk = s.psk?.takeIf { it.isNotEmpty() } ?: Prefs.apPsk.takeIf { it.isNotEmpty() },
                name = Net.deviceName(ctx)
            )
        }
    }

    private var server: Socks5Server? = null
    private var ble: BleHost? = null
    private var lohs: WifiManager.LocalOnlyHotspotReservation? = null
    private var lohsSsid: String? = null
    private var lohsPsk: String? = null
    private var lohsFailed = false
    private var lohsStarting = false
    private var wake: PowerManager.WakeLock? = null
    private var scope: CoroutineScope? = null
    private var lastBleTry = 0L
    private var lastNotifCount = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            shutdown()
            stopSelf()
            return START_NOT_STICKY
        }
        createChannels()
        startForeground(1, buildNotification())
        launch()
        return START_STICKY
    }

    private fun launch() {
        stopWork()
        val sc = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope = sc
        val code = Prefs.code
        val srv = Socks5Server(code, Pairing.portFor(code))
        try {
            srv.start()
        } catch (e: Exception) {
            // most often: the port derived from the code is used by another app -> the UI offers "new code"
            stopWork()
            state.value = HostInfo(error = e.message ?: "bind failed")
            stopForeground(true)
            stopSelf()
            return
        }
        server = srv
        state.value = HostInfo(running = true)

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wake = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "hotshare:host").apply { setReferenceCounted(false); acquire(6 * 60 * 60 * 1000L) }

        sc.launch {
            while (isActive) {
                manageBle(sc)
                val sysIp = Net.hotspotIp().takeIf { lohs == null }
                val sysOn = sysIp != null
                if (!sysOn && lohs == null && !lohsFailed && !lohsStarting && Prefs.autoHotspot) maybeStartLohs()
                val ap = when {
                    lohs != null -> ApMode.LOCAL
                    sysOn -> ApMode.SYSTEM
                    else -> ApMode.NONE
                }
                val ip = if (ap == ApMode.LOCAL) Net.hotspotIp() else sysIp
                val ssid = if (ap == ApMode.LOCAL) lohsSsid
                    else Prefs.apSsid.takeIf { it.isNotEmpty() } ?: if (ap == ApMode.SYSTEM) Net.hotspotSsidGuess(this@HostService) else null
                val psk = if (ap == ApMode.LOCAL) lohsPsk else Prefs.apPsk.takeIf { it.isNotEmpty() }
                val devices = srv.clients()
                state.value = state.value.copy(
                    running = true, clients = devices.size, devices = devices, up = srv.up.get(), down = srv.down.get(),
                    ip = ip, ssid = ssid, psk = psk, ap = ap, apFailed = lohsFailed && ap == ApMode.NONE,
                    ble = ble?.isAdvertising == true, bleState = bleState(), vpn = Net.isVpnActive(this@HostService)
                )
                if (devices.size != lastNotifCount) {
                    lastNotifCount = devices.size
                    runCatching { (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(1, buildNotification(devices.size)) }
                }
                delay(1000)
            }
        }
    }

    private fun bleState(): BleState = when {
        !Net.bleAdvertiseSupported(this) -> BleState.UNSUPPORTED
        !Perms.has(this, Perms.bleHost()) -> BleState.NO_PERMISSION
        !Net.bluetoothOn(this) -> BleState.BT_OFF
        ble?.isAdvertising == true -> BleState.ON
        else -> BleState.STARTING
    }

    /**
     * Keeps Bluetooth visibility in sync with reality: starts advertising as soon as Bluetooth / the permission
     * becomes available (even after sharing already started) and stops it when Bluetooth is switched off.
     */
    private fun manageBle(sc: CoroutineScope) {
        when (bleState()) {
            BleState.UNSUPPORTED, BleState.NO_PERMISSION -> { ble?.stop(); ble = null }
            BleState.BT_OFF -> { ble?.stop(); ble = null }
            BleState.STARTING, BleState.ON -> {
                val b = ble
                val stuck = b != null && !b.isAdvertising && System.currentTimeMillis() - lastBleTry > 8_000
                if (b == null || stuck) {
                    if (System.currentTimeMillis() - lastBleTry > 3_000) { lastBleTry = System.currentTimeMillis(); startBle(sc) }
                }
            }
        }
    }

    private fun startBle(sc: CoroutineScope) {
        ble?.stop(); ble = null
        val b = BleHost(
            ctx = applicationContext,
            scope = sc,
            name = { Net.deviceName(applicationContext) },
            offer = { Pairing.toUri(currentOffer(applicationContext)) },
            ask = { id, who -> ask(id, who) }
        )
        if (b.start()) ble = b
    }

    private suspend fun ask(id: String?, who: String): Boolean {
        if (Prefs.autoApprove) return true
        if (id != null && Prefs.isTrusted(id)) return true      // "always allow" was chosen for this device before
        val req = ApprovalRequest(who, canRemember = id != null)
        pending.value = req
        notifyAsk(who)
        val choice = withTimeoutOrNull(60_000) { req.result.await() } ?: ApprovalChoice.DENY
        if (pending.value === req) pending.value = null
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(2)
        if (choice == ApprovalChoice.ALWAYS && id != null) Prefs.trust(id, who)
        return choice != ApprovalChoice.DENY
    }

    @SuppressLint("MissingPermission")
    private fun maybeStartLohs() {
        if (Build.VERSION.SDK_INT < 26 || !Perms.has(this, Perms.hotspot())) { lohsFailed = true; return }
        lohsStarting = true
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        try {
            wm.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                override fun onStarted(r: WifiManager.LocalOnlyHotspotReservation) {
                    lohs = r
                    readCreds(r)
                    lohsStarting = false
                }
                override fun onStopped() { lohs = null; lohsSsid = null; lohsPsk = null; lohsStarting = false }
                override fun onFailed(reason: Int) { lohs = null; lohsFailed = true; lohsStarting = false }
            }, Handler(Looper.getMainLooper()))
        } catch (_: Exception) {
            lohsFailed = true; lohsStarting = false
        }
    }

    @Suppress("DEPRECATION")
    private fun readCreds(r: WifiManager.LocalOnlyHotspotReservation) {
        runCatching {
            if (Build.VERSION.SDK_INT >= 30) {
                val c = r.softApConfiguration
                lohsSsid = c.ssid; lohsPsk = c.passphrase
            } else {
                val c = r.wifiConfiguration
                lohsSsid = c?.SSID; lohsPsk = c?.preSharedKey
            }
            lohsSsid = lohsSsid?.trim('"'); lohsPsk = lohsPsk?.trim('"')
        }
    }

    private fun stopWork() {
        scope?.cancel(); scope = null
        ble?.stop(); ble = null
        server?.stop(); server = null
        wake?.let { if (it.isHeld) it.release() }; wake = null
    }

    private fun shutdown(keepError: Boolean = false) {
        stopWork()
        runCatching { lohs?.close() }
        lohs = null; lohsSsid = null; lohsPsk = null; lohsFailed = false; lohsStarting = false
        pending.value?.result?.complete(ApprovalChoice.DENY); pending.value = null
        if (!(keepError && state.value.error != null)) state.value = HostInfo()
    }

    override fun onDestroy() {
        shutdown(keepError = true)
        super.onDestroy()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CH, "Sharing", NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(CH_ASK, "Connection requests", NotificationManager.IMPORTANCE_HIGH))
    }

    private fun openApp() = PendingIntent.getActivity(
        this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun buildNotification(clients: Int = 0): Notification {
        val s = Strings.system()
        val stop = PendingIntent.getService(
            this, 1, Intent(this, HostService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CH)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(s.notifTitle)
            .setContentText(if (clients > 0) s.notifConnected(clients) else s.notifText)
            .setContentIntent(openApp())
            .addAction(0, s.notifStop, stop)
            .setOngoing(true)
            .build()
    }

    @SuppressLint("MissingPermission")
    private fun notifyAsk(who: String) {
        val s = Strings.system()
        val n = NotificationCompat.Builder(this, CH_ASK)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(s.notifAskTitle)
            .setContentText("$who ${s.approveBody}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(openApp())
            .setAutoCancel(true)
            .build()
        runCatching { (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(2, n) }
    }
}
