package ir.mrpc.hotshare.ui

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import ir.mrpc.hotshare.core.Offer
import ir.mrpc.hotshare.core.Pairing
import ir.mrpc.hotshare.core.Perms
import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

private tailrec fun Context.findOwner(): LifecycleOwner? = when (this) {
    is LifecycleOwner -> this
    is ContextWrapper -> baseContext.findOwner()
    else -> null
}

/** A decoded QR: its bounds in *upright image* coordinates plus the image size. */
private class Hit(val text: String?, val box: Rect?, val imgW: Float, val imgH: Float)

private class QrAnalyzer(val onHit: (Hit?) -> Unit) : ImageAnalysis.Analyzer {
    private val reader = MultiFormatReader().apply {
        setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE), DecodeHintType.TRY_HARDER to true))
    }

    override fun analyze(image: ImageProxy) {
        try {
            val plane = image.planes[0]
            val buf = plane.buffer
            val data = ByteArray(buf.remaining()); buf.get(data)
            val src = PlanarYUVLuminanceSource(data, plane.rowStride, image.height, 0, 0, image.width, image.height, false)
            val r = reader.decodeWithState(BinaryBitmap(HybridBinarizer(src)))
            val w = image.width.toFloat(); val h = image.height.toFloat()
            val rot = image.imageInfo.rotationDegrees
            fun up(p: ResultPoint): Offset = when (rot) {
                90 -> Offset(h - p.y, p.x)
                180 -> Offset(w - p.x, h - p.y)
                270 -> Offset(p.y, w - p.x)
                else -> Offset(p.x, p.y)
            }
            val pts = r.resultPoints.map { up(it) }
            val uw = if (rot == 90 || rot == 270) h else w
            val uh = if (rot == 90 || rot == 270) w else h
            if (pts.size >= 3) {
                val l = pts.minOf { it.x }; val rr = pts.maxOf { it.x }
                val t = pts.minOf { it.y }; val b = pts.maxOf { it.y }
                val side = max(rr - l, b - t) * 1.32f
                val cx = (l + rr) / 2f; val cy = (t + b) / 2f
                onHit(Hit(r.text, Rect(cx - side / 2, cy - side / 2, cx + side / 2, cy + side / 2), uw, uh))
            } else onHit(Hit(r.text, null, uw, uh))
        } catch (_: NotFoundException) {
            onHit(null)
        } catch (_: Exception) {
            onHit(null)
        } finally {
            reader.reset()
            image.close()
        }
    }
}

@Composable
fun ScannerScreen(nav: Nav, onOffer: (Offer) -> Unit) {
    val s = LocalStrings.current
    val hs = LocalHs.current
    val ctx = LocalContext.current
    var granted by remember { mutableStateOf(Perms.camera(ctx)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (granted) CameraScanner(onOffer, s.scanPrompt, s.scanInvalid, s.torch) else {
            Column(Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(s.camPerm, color = Color.White, textAlign = TextAlign.Center, fontSize = 15.sp)
                GradientButton(s.permGrant, { launcher.launch(Manifest.permission.CAMERA) })
            }
        }
        TopBar(s.scanTitle, onBack = nav.back, tint = Color.White)
    }
}

@Composable
private fun CameraScanner(onOffer: (Offer) -> Unit, prompt: String, invalidMsg: String, torchLabel: String) {
    val ctx = LocalContext.current
    val owner = remember { ctx.findOwner() }
    val executor = remember { Executors.newSingleThreadExecutor() }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var torch by remember { mutableStateOf(false) }

    var target by remember { mutableStateOf<Rect?>(null) }      // window position, in view pixels
    var state by remember { mutableIntStateOf(0) }              // 0 searching, 1 found, 2 invalid
    var viewW by remember { mutableFloatStateOf(0f) }
    var viewH by remember { mutableFloatStateOf(0f) }
    var lastSeen by remember { mutableLongStateOf(0L) }
    var found by remember { mutableStateOf<Offer?>(null) }
    val onOfferState by rememberUpdatedState(onOffer)

    fun handle(hit: Hit?) {
        if (found != null || hit == null) return
        val box = hit.box
        if (box != null && viewW > 0f && viewH > 0f) {
            // upright image -> view, PreviewView FILL_CENTER
            val scale = max(viewW / hit.imgW, viewH / hit.imgH)
            val dx = (viewW - hit.imgW * scale) / 2f
            val dy = (viewH - hit.imgH * scale) / 2f
            target = Rect(box.left * scale + dx, box.top * scale + dy, box.right * scale + dx, box.bottom * scale + dy)
        }
        lastSeen = System.currentTimeMillis()
        val offer = hit.text?.let { Pairing.parse(it) }
        if (offer != null) { state = 1; found = offer } else state = 2
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { camera?.cameraControl?.enableTorch(false) }
            runCatching { provider?.unbindAll() }
            executor.shutdown()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { c ->
                val pv = PreviewView(c).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
                val future = ProcessCameraProvider.getInstance(c)
                future.addListener({
                    val lifecycle = owner ?: return@addListener
                    val p = runCatching { future.get() }.getOrNull() ?: return@addListener
                    provider = p
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(pv.surfaceProvider) }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { it.setAnalyzer(executor, QrAnalyzer { hit -> pv.post { handle(hit) } }) }
                    runCatching {
                        p.unbindAll()
                        camera = p.bindToLifecycle(lifecycle, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                    }
                }, ContextCompat.getMainExecutor(c))
                pv
            }
        )

        // dimmed scrim with a rounded-square window that follows the QR code (like Telegram)
        Overlay(target, state) { w, h -> viewW = w; viewH = h }

        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                if (state == 2) invalidMsg else prompt,
                color = if (state == 2) Rose else Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Box(
                Modifier.size(58.dp).pressable {
                    torch = !torch
                    camera?.cameraControl?.enableTorch(torch)
                }.clip(CircleShape).background(if (torch) Amber else Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(if (torch) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff, torchLabel, tint = if (torch) Color.Black else Color.White)
            }
        }
    }

    // when the QR leaves the frame, the window glides back to the center
    LaunchedEffect(Unit) {
        while (true) {
            delay(250)
            if (found == null && lastSeen != 0L && System.currentTimeMillis() - lastSeen > 700) {
                target = null; state = 0; lastSeen = 0L
            }
        }
    }
    // hand over the result once the window has locked onto the code
    LaunchedEffect(found) {
        val f = found ?: return@LaunchedEffect
        delay(550)
        onOfferState(f)
    }
}

@Composable
private fun Overlay(target: Rect?, state: Int, onSize: (Float, Float) -> Unit) {
    val color by animateColorAsState(
        when (state) { 1 -> Mint; 2 -> Rose; else -> Color.White }, tween(250), label = "ovColor"
    )
    val t = rememberInfiniteTransition(label = "ov")
    val breathe by t.animateFloat(0.97f, 1.03f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathe")
    // animated window geometry
    var cw by remember { mutableFloatStateOf(0f) }
    var ch by remember { mutableFloatStateOf(0f) }
    val def = if (cw > 0f) {
        val side = min(cw, ch) * 0.66f
        Rect(cw / 2 - side / 2, ch * 0.42f - side / 2, cw / 2 + side / 2, ch * 0.42f + side / 2)
    } else Rect(0f, 0f, 0f, 0f)
    val goal = target ?: def
    val spec = spring<Float>(dampingRatio = 0.72f, stiffness = 420f)
    val l by animateFloatAsState(goal.left, spec, label = "l")
    val tp by animateFloatAsState(goal.top, spec, label = "t")
    val r by animateFloatAsState(goal.right, spec, label = "r")
    val b by animateFloatAsState(goal.bottom, spec, label = "b")

    Canvas(
        Modifier.fillMaxSize()
            .onSizeChanged { cw = it.width.toFloat(); ch = it.height.toFloat(); onSize(cw, ch) }
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    ) {
        drawRect(Color.Black.copy(alpha = 0.55f))
        val k = if (target == null && state == 0) breathe else 1f
        val cx = (l + r) / 2f; val cy = (tp + b) / 2f
        val w = (r - l) * k; val h = (b - tp) * k
        val rect = Rect(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2)
        val radius = min(w, h) * 0.12f
        drawRoundRect(Color.Black, rect.topLeft, Size(w, h), CornerRadius(radius), blendMode = BlendMode.Clear)

        // corner brackets
        val arm = min(w, h) * 0.22f
        val sw = 6.dp.toPx()
        fun corner(x: Float, y: Float, sx: Float, sy: Float) {
            val p = Path().apply {
                moveTo(x, y + sy * arm)
                lineTo(x, y + sy * radius)
                quadraticBezierTo(x, y, x + sx * radius, y)
                lineTo(x + sx * arm, y)
            }
            drawPath(p, color, style = Stroke(sw, cap = StrokeCap.Round))
        }
        corner(rect.left, rect.top, 1f, 1f)
        corner(rect.right, rect.top, -1f, 1f)
        corner(rect.left, rect.bottom, 1f, -1f)
        corner(rect.right, rect.bottom, -1f, -1f)
    }
}
