package ir.mrpc.hotshare.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import kotlinx.coroutines.delay

/* ───────────── Background ───────────── */

@Composable
fun AuroraBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val hs = LocalHs.current
    val t = rememberInfiniteTransition(label = "aurora")
    val a by t.animateFloat(0f, 1f, infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Reverse), label = "a")
    val b by t.animateFloat(0f, 1f, infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Reverse), label = "b")
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(hs.bg, hs.bg2, hs.bg)))
            .drawBehind {
                val w = size.width; val h = size.height
                val k = if (hs.dark) 0.34f else 0.22f
                fun blob(c: Color, cx: Float, cy: Float, r: Float) =
                    drawCircle(Brush.radialGradient(listOf(c.copy(alpha = k), Color.Transparent), Offset(cx, cy), r), r, Offset(cx, cy))
                blob(Violet, w * (0.15f + 0.6f * a), h * (0.12f + 0.12f * b), w * 0.85f)
                blob(Cyan, w * (0.95f - 0.5f * b), h * (0.55f + 0.1f * a), w * 0.8f)
                blob(Pink, w * (0.1f + 0.3f * b), h * (0.95f - 0.2f * a), w * 0.75f)
            },
        content = content
    )
}

/* ───────────── Modifiers ───────────── */

fun Modifier.appear(index: Int = 0): Modifier = composed {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 70L)
        p.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
    }
    graphicsLayer { alpha = p.value; translationY = (1f - p.value) * 48f }
}

fun Modifier.pressable(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.95f else 1f, spring(dampingRatio = 0.55f, stiffness = 500f), label = "press")
    graphicsLayer { scaleX = s; scaleY = s }.clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick)
}

/* ───────────── Cards & buttons ───────────── */

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = 24.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val hs = LocalHs.current
    val shape = RoundedCornerShape(radius)
    Box(
        modifier
            .let { if (onClick != null) it.pressable(onClick = onClick) else it }
            .clip(shape)
            .background(hs.card)
            .border(1.dp, hs.cardStroke, shape),
        content = content
    )
}

@Composable
fun GradientButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    val hs = LocalHs.current
    Box(
        modifier
            .fillMaxWidth()
            .height(58.dp)
            .pressable(enabled, onClick)
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .clip(RoundedCornerShape(20.dp))
            .background(hs.brand),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (icon != null) Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, tint: Color = LocalHs.current.text) {
    val hs = LocalHs.current
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .height(58.dp)
            .pressable(onClick = onClick)
            .clip(shape)
            .background(hs.card)
            .border(1.dp, hs.cardStroke, shape),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 14.dp)) {
            if (icon != null) Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            Text(text, color = tint, fontWeight = FontWeight.Medium, fontSize = 15.sp, maxLines = 1)
        }
    }
}

@Composable
fun IconBubble(icon: ImageVector, color: Color, size: Dp = 44.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(Brush.linearGradient(listOf(color.copy(alpha = 0.95f), color.copy(alpha = 0.55f)))),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(size * 0.54f)) }
}

@Composable
fun BrandMark(size: Dp) {
    val hs = LocalHs.current
    Box(Modifier.size(size).clip(CircleShape).background(hs.brand), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.Shield, null, tint = Color.White, modifier = Modifier.size(size * 0.52f))
    }
}

@Composable
fun StatusChip(icon: ImageVector, label: String, value: String, on: Boolean, modifier: Modifier = Modifier) {
    val hs = LocalHs.current
    val c by animateColorAsState(if (on) Mint else hs.sub, tween(400), label = "chip")
    GlassCard(modifier, radius = 20.dp) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, null, tint = c, modifier = Modifier.size(18.dp))
                Text(label, color = hs.sub, fontSize = 12.sp, maxLines = 1)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(c))
                Text(value, color = hs.text, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text.uppercase(), color = LocalHs.current.sub, fontSize = 12.sp, fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp, modifier = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 2.dp)
    )
}

/* ───────────── Rings / pulses ───────────── */

@Composable
fun PulseRings(modifier: Modifier = Modifier, color: Color = Cyan, rings: Int = 3, periodMs: Int = 3600) {
    val t = rememberInfiniteTransition(label = "pulse")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(periodMs, easing = LinearEasing)), label = "p")
    Canvas(modifier) {
        val r = size.minDimension / 2f
        repeat(rings) { i ->
            val q = (p + i.toFloat() / rings) % 1f
            drawCircle(color.copy(alpha = (1f - q) * 0.45f), radius = r * (0.35f + 0.65f * q), style = Stroke(width = 2.5f + 3f * (1f - q)))
        }
    }
}

/* ───────────── Stage list ───────────── */

enum class StageState { PENDING, ACTIVE, DONE }

@Composable
fun StageRow(title: String, state: StageState) {
    val hs = LocalHs.current
    val bg by animateColorAsState(
        when (state) { StageState.DONE -> Mint; StageState.ACTIVE -> Cyan; else -> hs.cardStroke }, tween(350), label = "stagebg"
    )
    val a by animateFloatAsState(if (state == StageState.PENDING) 0.5f else 1f, tween(300), label = "stagea")
    Row(
        Modifier.fillMaxWidth().graphicsLayer { alpha = a }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(bg.copy(alpha = 0.25f)).border(1.5.dp, bg, CircleShape), contentAlignment = Alignment.Center) {
            when (state) {
                StageState.DONE -> Icon(Icons.Rounded.Check, null, tint = Mint, modifier = Modifier.size(18.dp))
                StageState.ACTIVE -> CircularProgressIndicator(Modifier.size(16.dp), color = Cyan, strokeWidth = 2.dp)
                else -> {}
            }
        }
        Text(title, color = hs.text, fontWeight = if (state == StageState.ACTIVE) FontWeight.Bold else FontWeight.Normal, fontSize = 15.sp)
    }
}

/* ───────────── Code boxes + keypad ───────────── */

@Composable
fun DigitBoxes(code: String, length: Int = 8, animate: Boolean = true, boxWidth: Dp = 34.dp) {
    val hs = LocalHs.current
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            for (g in 0 until length / 4) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (k in 0 until 4) {
                        val idx = g * 4 + k
                        val ch = code.getOrNull(idx)
                        val shown = remember { Animatable(0f) }
                        LaunchedEffect(ch) {
                            if (ch != null) { shown.snapTo(0.6f); shown.animateTo(1f, spring(0.45f, 600f)) } else shown.snapTo(0f)
                        }
                        val active = idx == code.length
                        val shape = RoundedCornerShape(12.dp)
                        Box(
                            Modifier
                                .width(boxWidth).height(boxWidth * 1.35f)
                                .graphicsLayer { scaleX = if (animate) shown.value.coerceAtLeast(0.6f) else 1f; scaleY = scaleX }
                                .clip(shape)
                                .background(if (ch != null) hs.brand else Brush.linearGradient(listOf(hs.card, hs.card)))
                                .border(1.5.dp, if (active) Cyan else hs.cardStroke, shape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (ch != null) Text(ch.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = (boxWidth.value * 0.62f).sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Keypad(onDigit: (Char) -> Unit, onBackspace: () -> Unit) {
    val hs = LocalHs.current
    val haptic = LocalHapticFeedback.current
    val rows = listOf("123", "456", "789")
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            @Composable
            fun key(label: String?, icon: ImageVector? = null, action: () -> Unit) {
                if (label == null && icon == null) { Spacer(Modifier.size(70.dp)); return }
                Box(
                    Modifier.size(70.dp).pressable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); action()
                    }.clip(CircleShape).background(hs.card).border(1.dp, hs.cardStroke, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (label != null) Text(label, color = hs.text, fontWeight = FontWeight.Medium, fontSize = 26.sp)
                    if (icon != null) Icon(icon, null, tint = hs.text, modifier = Modifier.size(26.dp))
                }
            }
            rows.forEach { r ->
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) { r.forEach { d -> key(d.toString()) { onDigit(d) } } }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                key(null) {}
                key("0") { onDigit('0') }
                key(null, Icons.AutoMirrored.Rounded.Backspace) { onBackspace() }
            }
        }
    }
}

/* ───────────── Styled QR code ───────────── */

@Composable
fun QrCodeView(text: String, modifier: Modifier = Modifier, logo: @Composable BoxScope.() -> Unit = {}) {
    val matrix = remember(text) {
        runCatching {
            Encoder.encode(text, ErrorCorrectionLevel.Q, mapOf(EncodeHintType.CHARACTER_SET to "UTF-8")).matrix
        }.getOrNull()
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        if (matrix != null) {
            val n = matrix.width
            Canvas(Modifier.fillMaxSize()) {
                val quiet = 1
                val cell = size.minDimension / (n + quiet * 2)
                val ox = (size.width - cell * n) / 2f
                val oy = (size.height - cell * n) / 2f
                val brush = Brush.linearGradient(listOf(Color(0xFF0B7285), Color(0xFF5B21B6), Color(0xFF9D174D)), Offset.Zero, Offset(size.width, size.height))
                fun inFinder(x: Int, y: Int) = (x < 7 && y < 7) || (x >= n - 7 && y < 7) || (x < 7 && y >= n - 7)
                val c0 = n * 0.5f - n * 0.1f
                val c1 = n * 0.5f + n * 0.1f
                for (y in 0 until n) for (x in 0 until n) {
                    if (matrix.get(x, y).toInt() != 1 || inFinder(x, y)) continue
                    if (x >= c0 && x <= c1 && y >= c0 && y <= c1) continue
                    drawRoundRect(
                        brush, Offset(ox + x * cell + cell * 0.04f, oy + y * cell + cell * 0.04f),
                        Size(cell * 0.92f, cell * 0.92f), CornerRadius(cell * 0.32f)
                    )
                }
                fun finder(fx: Int, fy: Int) {
                    val x = ox + fx * cell; val y = oy + fy * cell
                    drawRoundRect(brush, Offset(x + cell / 2, y + cell / 2), Size(cell * 6, cell * 6), CornerRadius(cell * 1.8f), style = Stroke(cell))
                    drawRoundRect(brush, Offset(x + cell * 2, y + cell * 2), Size(cell * 3, cell * 3), CornerRadius(cell * 1.0f))
                }
                finder(0, 0); finder(n - 7, 0); finder(0, n - 7)
            }
            Box(
                Modifier.fillMaxSize(0.2f).clip(RoundedCornerShape(14.dp)).background(Color.White),
                contentAlignment = Alignment.Center, content = logo
            )
        }
    }
}

/* ───────────── Misc ───────────── */

fun formatBytes(b: Long): String = when {
    b >= 1L shl 30 -> String.format("%.2f GB", b / (1L shl 30).toDouble())
    b >= 1L shl 20 -> String.format("%.1f MB", b / (1L shl 20).toDouble())
    b >= 1L shl 10 -> String.format("%.0f KB", b / (1L shl 10).toDouble())
    else -> "$b B"
}

fun formatDuration(ms: Long): String {
    val s = ms / 1000
    return String.format("%02d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60)
}
