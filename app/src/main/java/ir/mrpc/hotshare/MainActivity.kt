package ir.mrpc.hotshare

import android.Manifest
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
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import ir.mrpc.hotshare.ble.NearbyDevice
import ir.mrpc.hotshare.client.ConnError
import ir.mrpc.hotshare.client.ConnFlow
import ir.mrpc.hotshare.client.Connector
import ir.mrpc.hotshare.core.Net
import ir.mrpc.hotshare.core.Offer
import ir.mrpc.hotshare.core.Perms
import ir.mrpc.hotshare.core.Prefs
import ir.mrpc.hotshare.host.ApprovalChoice
import ir.mrpc.hotshare.host.HostService
import ir.mrpc.hotshare.ui.*

class MainActivity : ComponentActivity() {

    private var permCallback: ((Boolean) -> Unit)? = null
    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        val cb = permCallback
        permCallback = null
        cb?.invoke(r.values.all { it })
    }

    private var afterVpn: (() -> Unit)? = null
    private val vpnLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == RESULT_OK) afterVpn?.invoke() else Connector.failExternal(ConnError.VPN_DENIED)
        afterVpn = null
    }

    private var btCallback: ((Boolean) -> Unit)? = null
    private val btLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val cb = btCallback
        btCallback = null
        cb?.invoke(r.resultCode == RESULT_OK)
    }

    /**
     * Apps can't switch Bluetooth on silently; Android shows its own "Turn on Bluetooth?" dialog.
     * On Android 12+ that needs the Bluetooth-connect permission first, so we ask for it by ourselves.
     */
    private fun askBluetooth(done: (Boolean) -> Unit = {}) {
        if (Net.bluetoothOn(this)) { done(true); return }
        fun launchDialog() {
            btCallback = done
            val ok = runCatching { btLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) }.isSuccess
            if (!ok) {
                btCallback = null
                Net.openBluetoothSettings(this)
                done(false)
            }
        }
        if (Build.VERSION.SDK_INT >= 31) {
            requestPerms(arrayOf(Manifest.permission.BLUETOOTH_CONNECT)) { granted -> if (granted) launchDialog() else done(false) }
        } else launchDialog()
    }

    private var locCallback: ((Boolean) -> Unit)? = null
    private val locLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        val cb = locCallback
        locCallback = null
        cb?.invoke(r.resultCode == RESULT_OK || Net.locationOn(this))
    }

    /**
     * Turns Location on with Android's own one-tap "Turn on location?" dialog (needs Google Play services).
     * Without it (or if it fails) the phone's Location settings page opens directly.
     */
    private fun askLocation(done: (Boolean) -> Unit = {}) {
        if (Net.locationOn(this)) { done(true); return }
        fun fallback() { Net.openLocationSettings(this); done(false) }
        runCatching {
            val req = com.google.android.gms.location.LocationSettingsRequest.Builder()
                .addLocationRequest(com.google.android.gms.location.LocationRequest.Builder(
                    com.google.android.gms.location.Priority.PRIORITY_LOW_POWER, 10_000L).build())
                .build()
            com.google.android.gms.location.LocationServices.getSettingsClient(this).checkLocationSettings(req)
                .addOnSuccessListener { done(true) }
                .addOnFailureListener { e ->
                    val ex = e as? com.google.android.gms.common.api.ResolvableApiException
                    if (ex == null) fallback()
                    else {
                        locCallback = done
                        val ok = runCatching {
                            locLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(ex.resolution).build())
                        }.isSuccess
                        if (!ok) { locCallback = null; fallback() }
                    }
                }
        }.onFailure { fallback() }
    }

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
                // permissions -> Bluetooth prompt (so nearby phones can see us) -> start. QR / code still work if Bluetooth is declined.
                requestPerms(Perms.hostAll()) { askBluetooth { HostService.start(this) } }
            },
            stopHost = { HostService.stop(this) },
            connectDevice = { d: NearbyDevice, h: BluetoothDevice? ->
                if (h == null) Connector.failExternal(ConnError.BLE)
                else withVpn(d.name, true) { Connector.connectDevice(d, h) }
            },
            connectOffer = { o: Offer -> withVpn(o.name, false) { Connector.connectOffer(o) } },
            connectCode = { c: String -> withVpn(null, false) { Connector.connectCode(c) } },
            requestPerms = { p, cb -> requestPerms(p, cb) },
            enableBluetooth = { askBluetooth() },
            enableLocation = { cb -> askLocation(cb) }
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
    val active = hostInfo.running || flow is ConnFlow.Done || flow is ConnFlow.Working || flow is ConnFlow.NeedWifi
    LaunchedEffect(active, Prefs.keepScreenOn) {
        if (active && Prefs.keepScreenOn) activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    run {
        AuroraBackground {
            // Content on top, fixed bottom bar underneath: they never overlap.
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
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
                                Screen.SPLASH -> Splash {
                                    // first launch (or when switched on in Menu -> General) the single tutorial opens first
                                    stack.clear(); stack.add(Screen.HOME)
                                    if (Prefs.showTutorialOnStart()) stack.add(Screen.ONBOARD)
                                }
                                Screen.HOME -> HomeScreen(nav, actions)
                                Screen.MENU -> MenuScreen(nav)
                                Screen.HELP -> HelpScreen(nav)
                                Screen.ONBOARD -> OnboardingScreen { nav.back() }
                                Screen.SET_SHARE -> ShareSettingsScreen(nav)
                                Screen.SET_LOOK -> LookSettingsScreen(nav)
                                Screen.SET_GENERAL -> GeneralSettingsScreen(nav)
                                Screen.HELP_WHICH -> HelpWhichScreen(nav)
                                Screen.HELP_METHODS -> HelpMethodsScreen(nav)
                                Screen.HELP_STEPS -> HelpStepsScreen(nav)
                                Screen.HELP_WIFI -> HelpWifiScreen(nav)
                                Screen.HELP_FAQ -> HelpFaqScreen(nav)
                                Screen.HOST -> HostScreen(nav, actions)
                                Screen.RECEIVE -> ReceiveScreen(nav, actions) { showCode = true }
                                Screen.SCAN -> ScannerScreen(nav) { offer ->
                                    // replace scanner by the connect screen
                                    stack.removeAt(stack.lastIndex); stack.add(Screen.CONNECT)
                                    actions.connectOffer(offer)
                                }
                                Screen.CONNECT -> ConnectScreen(nav, actions)
                                Screen.ABOUT -> AboutScreen(nav)
                                Screen.SETUP -> SetupScreen(nav, actions)
                                Screen.PRIVACY -> PrivacyScreen(nav)
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = current.tab,
                    enter = slideInVertically(tween(300)) { it } + fadeIn(),
                    exit = slideOutVertically(tween(200)) { it } + fadeOut()
                ) { BottomNavBar(current, { nav.tab(it) }) }

                // screens without the bar keep a clean gap above the phone's own navigation bar
                AnimatedVisibility(visible = !current.tab && current != Screen.SPLASH && current != Screen.SCAN && current != Screen.ONBOARD) {
                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
            }

            if (showCode) {
                CodeSheet(onDismiss = { showCode = false }, onSubmit = { code ->
                    showCode = false
                    if (stack.last() != Screen.CONNECT) stack.add(Screen.CONNECT)
                    actions.connectCode(code)
                })
            }

            pending?.let { req ->
                ApprovalDialog(
                    req.name, req.canRemember,
                    onOnce = { req.result.complete(ApprovalChoice.ONCE) },
                    onAlways = { req.result.complete(ApprovalChoice.ALWAYS) },
                    onDeny = { req.result.complete(ApprovalChoice.DENY) }
                )
            }
        }
    }

    // when a connection starts from the radar, jump to the progress screen
    LaunchedEffect(flow) {
        if (flow is ConnFlow.Working && stack.last() == Screen.RECEIVE) stack.add(Screen.CONNECT)
    }
}
