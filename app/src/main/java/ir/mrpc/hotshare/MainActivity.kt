package ir.mrpc.hotshare

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ir.mrpc.hotshare.ble.NearbyDevice
import ir.mrpc.hotshare.client.ConnError
import ir.mrpc.hotshare.client.ConnFlow
import ir.mrpc.hotshare.client.Connector
import ir.mrpc.hotshare.core.Net
import ir.mrpc.hotshare.core.Offer
import ir.mrpc.hotshare.core.Perms
import ir.mrpc.hotshare.core.Prefs
import ir.mrpc.hotshare.host.HostService
import ir.mrpc.hotshare.ui.*

class MainActivity : ComponentActivity() {

    private var permCallback: ((Boolean) -> Unit)? = null
    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        permCallback?.invoke(r.values.all { it })
        permCallback = null
    }

    private var afterVpn: (() -> Unit)? = null
    private val vpnLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == RESULT_OK) afterVpn?.invoke() else Connector.failExternal(ConnError.VPN_DENIED)
        afterVpn = null
    }

    private val btLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    private fun requestPerms(perms: Array<String>, cb: (Boolean) -> Unit) {
        if (perms.isEmpty() || Perms.has(this, perms)) { cb(true); return }
        permCallback = cb
        permLauncher.launch(perms)
    }

    /** VPN permission is a system dialog, so it must be asked before the pipeline starts. */
    private fun withVpn(host: String?, via: Boolean, block: () -> Unit) {
        Connector.markStarting(host, via)
        val i = VpnService.prepare(this)
        if (i == null) block() else { afterVpn = block; vpnLauncher.launch(i) }
    }

    private val actions by lazy {
        Actions(
            startHost = {
                requestPerms(Perms.hostAll()) { HostService.start(this) }
            },
            stopHost = { HostService.stop(this) },
            connectDevice = { d: NearbyDevice, h: BluetoothDevice? ->
                if (h == null) Connector.failExternal(ConnError.BLE)
                else withVpn(d.name, true) { Connector.connectDevice(d, h) }
            },
            connectOffer = { o: Offer -> withVpn(o.name, false) { Connector.connectOffer(o) } },
            connectCode = { c: String -> withVpn(null, false) { Connector.connectCode(c) } },
            requestPerms = { p, cb -> requestPerms(p, cb) },
            enableBluetooth = {
                runCatching { btLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) }
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { HotShareTheme { AppRoot(actions) } }
    }
}

@Composable
private fun AppRoot(actions: Actions) {
    val activity = androidx.compose.ui.platform.LocalContext.current as ComponentActivity
    val stack = remember { SnapshotStateList<Screen>().apply { add(Screen.SPLASH) } }
    val current = stack.last()

    val nav = remember {
        Nav(
            go = { stack.add(it) },
            back = { if (stack.size > 1) stack.removeAt(stack.lastIndex) },
            tab = { t -> stack.clear(); stack.add(Screen.HOME); if (t != Screen.HOME) stack.add(t) }
        )
    }
    var showCode by remember { mutableStateOf(false) }
    val hostInfo by HostService.state.collectAsState()
    val flow by Connector.flow.collectAsState()
    val pending by HostService.pending.collectAsState()

    BackHandler(enabled = current != Screen.SPLASH && current != Screen.HOME) {
        if (current == Screen.CONNECT) {
            when (flow) {
                is ConnFlow.Working, is ConnFlow.NeedWifi -> Connector.cancel()
                is ConnFlow.Failed -> Connector.reset()
                else -> {}
            }
        }
        nav.back()
    }

    // keep screen awake while sharing / connected
    val active = hostInfo.running || flow is ConnFlow.Done || flow is ConnFlow.Working
    LaunchedEffect(active, Prefs.keepScreenOn) {
        if (active && Prefs.keepScreenOn) activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    AuroraBackground {
        AnimatedContent(
            targetState = current,
            transitionSpec = {
                if (initialState == Screen.SPLASH) fadeIn(tween(500)) togetherWith fadeOut(tween(300))
                else if (targetState.tab && initialState.tab) fadeIn(tween(250)) togetherWith fadeOut(tween(150))
                else if (!targetState.tab && initialState.tab || (!targetState.tab && !initialState.tab && targetState.ordinal > initialState.ordinal))
                    (slideInHorizontally(tween(380)) { it / 3 } + fadeIn(tween(380))) togetherWith (slideOutHorizontally(tween(380)) { -it / 4 } + fadeOut(tween(250)))
                else (slideInHorizontally(tween(380)) { -it / 3 } + fadeIn(tween(380))) togetherWith (slideOutHorizontally(tween(380)) { it / 4 } + fadeOut(tween(250)))
            },
            label = "screens"
        ) { scr ->
            Box(Modifier.fillMaxSize()) {
                when (scr) {
                    Screen.SPLASH -> Splash { stack.clear(); stack.add(Screen.HOME) }
                    Screen.HOME -> HomeScreen(nav)
                    Screen.MENU -> MenuScreen(nav, actions) { showCode = true }
                    Screen.HELP -> HelpScreen()
                    Screen.HOST -> HostScreen(nav, actions)
                    Screen.RECEIVE -> ReceiveScreen(nav, actions) { showCode = true }
                    Screen.SCAN -> ScannerScreen(nav) { offer ->
                        // replace scanner by the connect screen
                        stack.removeAt(stack.lastIndex); stack.add(Screen.CONNECT)
                        actions.connectOffer(offer)
                    }
                    Screen.CONNECT -> ConnectScreen(nav)
                    Screen.ABOUT -> AboutScreen(nav)
                }
            }
        }

        // bottom floating navigation on top-level screens
        AnimatedVisibility(
            visible = current.tab,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
            enter = slideInVertically(tween(350)) { it } + fadeIn(),
            exit = slideOutVertically(tween(250)) { it } + fadeOut()
        ) { FloatingNav(current, { nav.tab(it) }) }

        if (showCode) {
            CodeSheet(onDismiss = { showCode = false }, onSubmit = { code ->
                showCode = false
                if (stack.last() != Screen.CONNECT) stack.add(Screen.CONNECT)
                actions.connectCode(code)
            })
        }

        pending?.let { req ->
            ApprovalDialog(req.name, onAllow = { req.result.complete(true) }, onDeny = { req.result.complete(false) })
        }
    }

    // when a connection starts from the radar, jump to the progress screen
    LaunchedEffect(flow) {
        if (flow is ConnFlow.Working && stack.last() == Screen.RECEIVE) stack.add(Screen.CONNECT)
    }
}
