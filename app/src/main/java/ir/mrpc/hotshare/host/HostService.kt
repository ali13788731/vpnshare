package ir.mrpc.hotshare.host

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import ir.mrpc.hotshare.MainActivity
import ir.mrpc.hotshare.R
import ir.mrpc.hotshare.core.Pairing
import ir.mrpc.hotshare.core.Prefs
import ir.mrpc.hotshare.core.Strings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

data class HostInfo(
    val running: Boolean = false,
    val clients: Int = 0,
    val up: Long = 0,
    val down: Long = 0,
    val error: String? = null
)

class HostService : Service() {
    companion object {
        val state = MutableStateFlow(HostInfo())
        private const val ACTION_STOP = "ir.mrpc.hotshare.STOP"
        private const val CHANNEL = "share"
        private const val ID = 17

        /** هر بار صدا زده شود سرور با کد فعلی از نو راه‌اندازی می‌شود. */
        fun start(ctx: Context) = ContextCompat.startForegroundService(ctx, Intent(ctx, HostService::class.java))
        fun stop(ctx: Context) { ctx.stopService(Intent(ctx, HostService::class.java)) }
    }

    private var server: Socks5Server? = null
    private var scope: CoroutineScope? = null
    private var wake: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        goForeground()

        server?.stop()
        val code = Prefs.code
        val srv = Socks5Server(code, Pairing.portFor(code))
        try {
            srv.start()
        } catch (e: Exception) {
            state.value = HostInfo(error = e.message ?: "error")
            stopSelf(); return START_NOT_STICKY
        }
        server = srv

        if (wake == null) {
            wake = (getSystemService(POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "hotshare:host").apply { acquire(6 * 60 * 60 * 1000L) }
        }
        scope?.cancel()
        scope = CoroutineScope(Dispatchers.Default + SupervisorJob()).also { sc ->
            sc.launch {
                while (isActive) {
                    state.value = HostInfo(true, srv.clientCount(), srv.up.get(), srv.down.get())
                    delay(1000)
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope?.cancel(); server?.stop(); server = null
        runCatching { wake?.release() }; wake = null
        state.value = HostInfo()
        super.onDestroy()
    }

    private fun goForeground() {
        val s = Strings(Prefs.language == "fa")
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, s.appName, NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, HostService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(s.notifTitle)
            .setContentText(s.notifText)
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(0, s.notifStop, stop)
            .build()
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, ID, n, type)
    }
}
