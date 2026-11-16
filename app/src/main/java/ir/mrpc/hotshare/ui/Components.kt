package ir.mrpc.hotshare.ui

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.delay
import java.util.Locale

/** لوگوی برنامه؛ progress از ۰ تا ۱ انیمیشن ظاهر شدن را کنترل می‌کند. */
@Composable
fun Logo(modifier: Modifier = Modifier, progress: Float = 1f) {
    Canvas(modifier) {
        val w = size.minDimension
        drawRoundRect(
            brush = Brush.linearGradient(listOf(Cyan, Violet)),
            cornerRadius = CornerRadius(w * 0.28f),
            alpha = (progress * 3f).coerceIn(0f, 1f)
        )
        val cx = size.width / 2
        val base = size.height * 0.68f
        val stroke = w * 0.075f
        for (i in 0..2) {
            val r = w * (0.16f + 0.13f * i)
            val p = (progress * 3.2f - 0.6f - i * 0.5f).coerceIn(0f, 1f)
            drawArc(
                color = Color.White,
                startAngle = 270f - 45f * p,
                sweepAngle = 90f * p,
                useCenter = false,
                topLeft = Offset(cx - r, base - r),
                size = Size(2 * r, 2 * r),
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        drawCircle(Color.White, radius = w * 0.055f * (progress * 3f).coerceIn(0f, 1f), center = Offset(cx, base))
    }
}

@Composable
fun AuroraBackground(content: @Composable BoxScope.() -> Unit) {
    val bg = MaterialTheme.colorScheme.background
    val dark = bg.luminance() < 0.5f
    val t = rememberInfiniteTransition(label = "bg")
    val a by t.animateFloat(0f, 1f, infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse), label = "a")
    Box(
        Modifier.fillMaxSize().background(bg).drawBehind {
            val w = size.width; val h = size.height
            val c1 = Offset(w * (0.15f + 0.5f * a), h * 0.10f)
            val c2 = Offset(w * (0.90f - 0.5f * a), h * (0.88f - 0.10f * a))
            drawCircle(Brush.radialGradient(listOf(Cyan.copy(alpha = if (dark) .28f else .20f), Color.Transparent), center = c1, radius = w), radius = w, center = c1)
            drawCircle(Brush.radialGradient(listOf(Violet.copy(alpha = if (dark) .28f else .18f), Color.Transparent), center = c2, radius = w), radius = w, center = c2)
        },
        content = content
    )
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    var m = modifier
        .clip(shape)
        .background(MaterialTheme.colorScheme.surface.copy(alpha = .72f))
        .border(1.dp, Brush.linearGradient(listOf(Cyan.copy(.5f), Violet.copy(.3f))), shape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(m.padding(20.dp), content = content)
}

@Composable
fun GradientButton(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (pressed) .96f else 1f, spring(), label = "press")
    val brush: Brush = if (enabled) Brush.horizontalGradient(listOf(Cyan, Violet)) else SolidColor(Color.Gray.copy(.35f))
    Box(
        modifier.scale(sc).height(56.dp).clip(RoundedCornerShape(18.dp)).background(brush)
            .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (icon != null) Icon(icon, null, tint = Color.White)
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun PulseRings(active: Boolean, modifier: Modifier = Modifier, color: Color = Cyan, content: @Composable () -> Unit) {
    val t = rememberInfiniteTransition(label = "pulse")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "p")
    Box(modifier, contentAlignment = Alignment.Center) {
        if (active) Canvas(Modifier.matchParentSize()) {
            for (i in 0..2) {
                val q = (p + i / 3f) % 1f
                drawCircle(color.copy(alpha = (1f - q) * .4f), radius = size.minDimension / 2 * (0.5f + 0.5f * q), style = Stroke(3.dp.toPx()))
            }
        }
        content()
    }
}

@Composable
fun StatusPill(text: String, color: Color) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = .16f)).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun Appear(delayMs: Int = 0, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(delayMs.toLong()); shown = true }
    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 5 }
    ) { content() }
}

@Composable
fun StepCard(number: Int, title: String, status: String, color: Color, content: @Composable ColumnScope.() -> Unit = {}) {
    GlassCard(Modifier.fillMaxWidth().animateContentSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Cyan, Violet))), contentAlignment = Alignment.Center) {
                Text("$number", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            StatusPill(status, color)
        }
        content()
    }
}

@Composable
fun TopBar(title: String, onBack: (() -> Unit)?, nav: ((Screen) -> Unit)?) {
    val s = LocalStrings.current
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) IconButton(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null) } else Spacer(Modifier.width(48.dp))
        Text(title, Modifier.weight(1f).padding(horizontal = 8.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (nav != null) {
            var open by remember { mutableStateOf(false) }
            Box {
                IconButton({ open = true }) { Icon(Icons.Rounded.MoreVert, null) }
                DropdownMenu(open, { open = false }) {
                    DropdownMenuItem(text = { Text(s.settings) }, leadingIcon = { Icon(Icons.Rounded.Settings, null) }, onClick = { open = false; nav(Screen.SETTINGS) })
                    DropdownMenuItem(text = { Text(s.helpAbout) }, leadingIcon = { Icon(Icons.Rounded.Info, null) }, onClick = { open = false; nav(Screen.ABOUT) })
                }
            }
        } else Spacer(Modifier.width(48.dp))
    }
}

@Composable
fun StatBox(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Cyan)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
    }
}

fun qrBitmap(text: String, size: Int = 640): ImageBitmap {
    val m = QRCodeWriter().encode(
        text, BarcodeFormat.QR_CODE, size, size,
        mapOf(EncodeHintType.MARGIN to 1, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M)
    )
    val px = IntArray(size * size) { if (m.get(it % size, it / size)) 0xFF0B1020.toInt() else 0xFFFFFFFF.toInt() }
    return Bitmap.createBitmap(px, size, size, Bitmap.Config.ARGB_8888).asImageBitmap()
}

fun fmtBytes(b: Long): String {
    if (b < 1024) return "$b B"
    val u = arrayOf("KB", "MB", "GB", "TB")
    var v = b / 1024.0; var i = 0
    while (v >= 1024 && i < u.size - 1) { v /= 1024; i++ }
    return String.format(Locale.US, "%.1f %s", v, u[i])
}

fun fmtTime(sec: Long) = String.format(Locale.US, "%02d:%02d:%02d", sec / 3600, (sec / 60) % 60, sec % 60)
