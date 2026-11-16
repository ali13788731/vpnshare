package ir.mrpc.hotshare

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import ir.mrpc.hotshare.client.ClientError
import ir.mrpc.hotshare.client.ClientState
import ir.mrpc.hotshare.client.ClientVpnService
import ir.mrpc.hotshare.core.Pairing
import ir.mrpc.hotshare.core.Prefs
import ir.mrpc.hotshare.core.Strings
import ir.mrpc.hotshare.host.HostService
import ir.mrpc.hotshare.ui.*

class Actions(
    val startHost: () -> Unit,
    val stopHost: () -> Unit,
    val scan: () -> Unit,
    val connect: (Pairing.Link) -> Unit,
    val disconnect: () -> Unit
)

class MainActivity : ComponentActivity() {
    private var pending: Pairing.Link? = null

    private val scanLauncher = registerForActivityResult(ScanContract()) { r ->
        val text = r.contents ?: return@registerForActivityResult
        val link = Pairing.parse(text)
        if (link == null) ClientVpnService.state.value = ClientState.Error(ClientError.BAD_QR) else beginConnect(link)
    }
    private val vpnLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == RESULT_OK) pending?.let { launchClient(it) }
        else ClientVpnService.state.value = ClientState.Error(ClientError.VPN_DENIED)
    }
    private val notifLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        HostService.start(this)   // حتی اگر اجازه ندهد، سرویس کار می‌کند
    }

    private fun beginConnect(link: Pairing.Link) {
        pending = link
        val i = VpnService.prepare(this)
        if (i == null) launchClient(link) else vpnLauncher.launch(i)
    }

    private fun launchClient(link: Pairing.Link) {
        ClientVpnService.state.value = ClientState.Connecting
        startService(
            Intent(this, ClientVpnService::class.java)
                .putExtra(ClientVpnService.EXTRA_IP, link.ip)
                .putExtra(ClientVpnService.EXTRA_CODE, link.code)
        )
    }

    private fun startHost() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            !Prefs.askedNotif
        ) {
            Prefs.askedNotif = true; Prefs.save()
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else HostService.start(this)
    }

    private val actions by lazy {
        Actions(
            startHost = ::startHost,
            stopHost = { HostService.stop(this) },
            scan = {
                scanLauncher.launch(
                    ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                        .setPrompt(Strings(Prefs.language == "fa").scanPrompt)
                        .setBeepEnabled(false).setOrientationLocked(false)
                )
            },
            connect = ::beginConnect,
            disconnect = {
                startService(Intent(this, ClientVpnService::class.java).setAction(ClientVpnService.ACTION_STOP))
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { HotShareTheme { AppRoot(actions) } }
    }
}

@Composable
private fun AppRoot(actions: Actions) {
    val stack = remember { mutableStateListOf(Screen.SPLASH) }
    val screen = stack.last()
    val host by HostService.state.collectAsState()
    val client by ClientVpnService.state.collectAsState()

    val view = LocalView.current
    SideEffect { view.keepScreenOn = Prefs.keepScreenOn && (host.running || client is ClientState.Connected) }

    fun go(s: Screen) { stack.add(s) }
    fun pop() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }

    BackHandler(enabled = stack.size > 1 && screen != Screen.SPLASH) { pop() }

    AnimatedContent(
        targetState = screen,
        transitionSpec = { (fadeIn(tween(350)) + slideInHorizontally(tween(350)) { it / 8 }) togetherWith fadeOut(tween(200)) },
        label = "nav"
    ) { s ->
        when (s) {
            Screen.SPLASH -> SplashScreen { stack.clear(); stack.add(Screen.ROLE) }
            Screen.ROLE -> RoleScreen(::go)
            Screen.HOST -> HostScreen(actions, ::go, ::pop)
            Screen.CLIENT -> ClientScreen(actions, ::go, ::pop)
            Screen.SETTINGS -> SettingsScreen(::pop) { Prefs.newCode(); if (host.running) actions.startHost() }
            Screen.ABOUT -> AboutScreen(::pop)
        }
    }
}
