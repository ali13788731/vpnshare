package ir.mrpc.hotshare.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.mrpc.hotshare.core.Strings
import kotlin.math.min
import kotlin.math.roundToInt

/* ───────────────────────────────────────────────────────────────────────────
 * Guided tour ("coach marks"): dims the screen, draws a ring around one element,
 * an arrow from the explanation card to that element, and walks the user step by step.
 * Mark an element with  Modifier.coachTarget("id")  and describe it in tourSteps().
 * ─────────────────────────────────────────────────────────────────────────── */

@Stable
class CoachState {
    val rects = mutableStateMapOf<String, Rect>()
    var active by mutableStateOf<String?>(null)
        private set

    fun start(id: String) { active = id }
    fun stop() { active = null }
}

val LocalCoach = staticCompositionLocalOf { CoachState() }

/** Registers this element's on-screen rectangle so a tour step can point at it. */
fun Modifier.coachTarget(id: String): Modifier = composed {
    val st = LocalCoach.current
    DisposableEffect(id) { onDispose { st.rects.remove(id) } }
    Modifier.onGloballyPositioned { st.rects[id] = it.boundsInRoot() }
}

class CoachStep(val id: String, val title: String, val body: String)

fun tourSteps(id: String, s: Strings): List<CoachStep> = when (id) {
    "home" -> listOf(
        CoachStep("home_share", s.tourShareTitle, s.tourShareBody),
        CoachStep("home_receive", s.tourReceiveTitle, s.tourReceiveBody),
        CoachStep("home_status", s.tourStatusTitle, s.tourStatusBody),
        CoachStep("nav_menu", s.tourMenuTitle, s.tourMenuBody),
        CoachStep("nav_help", s.tourHelpTitle, s.tourHelpBody)
    )
    "host" -> listOf(
        CoachStep("host_check", s.tourHostCheckTitle, s.tourHostCheckBody),
        CoachStep("host_start", s.tourHostStartTitle, s.tourHostStartBody),
        CoachStep("host_qr", s.tourHostQrTitle, s.tourHostQrBody),
        CoachStep("host_code", s.tourHostCodeTitle, s.tourHostCodeBody)
    )
    "receive" -> listOf(
        CoachStep("recv_radar", s.tourRadarTitle, s.tourRadarBody),
        CoachStep("recv_actions", s.tourRecvActionsTitle, s.tourRecvActionsBody)
    )
    else -> emptyList()
}

@Composable
fun CoachOverlay(steps: List<CoachStep>, state: CoachState, onFinish: () -> Unit) {
    if (steps.isEmpty()) {
        LaunchedEffect(Unit) { onFinish() }
        return
    }
    val s = LocalStrings.current
    val hs = LocalHs.current
    val density = LocalDensity.current

    var index by remember(steps) { mutableIntStateOf(0) }
    val step = steps[index.coerceIn(0, steps.lastIndex)]
    var screen by remember { mutableStateOf(IntSize.Zero) }
    var cardSize by remember { mutableStateOf(IntSize.Zero) }

    // the element we point at (ignored if it's currently off-screen)
    val raw = state.rects[step.id]
    val target = if (raw != null && screen.height > 0 && raw.bottom > 0f && raw.top < screen.height) raw else null
    val hole = target?.inflate(with(density) { 8.dp.toPx() })

    val hl by animateFloatAsState(hole?.left ?: 0f, tween(350), label = "hl")
    val ht by animateFloatAsState(hole?.top ?: 0f, tween(350), label = "ht")
    val hr by animateFloatAsState(hole?.right ?: 0f, tween(350), label = "hr")
    val hb by animateFloatAsState(hole?.bottom ?: 0f, tween(350), label = "hb")
    val show by animateFloatAsState(if (hole != null) 1f else 0f, tween(250), label = "show")

    val tr = rememberInfiniteTransition(label = "coach")
    val pulse by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")

    // explanation card: next to the target (above it when the target is in the lower half)
    val gap = with(density) { 72.dp.toPx() }
    val margin = with(density) { 16.dp.toPx() }
    val topInset = WindowInsets.statusBars.getTop(density).toFloat()
    val botInset = WindowInsets.navigationBars.getBottom(density).toFloat()
    val sh = screen.height.toFloat()
    val ch = cardSize.height.toFloat()
    val above = hole != null && hole.center.y > sh / 2f
    val wantY = when {
        hole == null -> (sh - ch) / 2f
        above -> (hole.top - gap - ch).coerceAtLeast(topInset + margin)
        else -> (hole.bottom + gap).coerceAtMost(sh - botInset - margin - ch)
    }
    val cardY by animateFloatAsState(wantY, tween(350), label = "cardY")

    Box(
        Modifier.fillMaxSize()
            .onSizeChanged { screen = it }
            .pointerInput(Unit) { detectTapGestures { } }   // swallow taps: the app below is not clickable during the tour
    ) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            drawRect(Color.Black.copy(alpha = 0.80f))
            if (show > 0.01f) {
                val w = hr - hl
                val h = hb - ht
                val cr = min(min(w, h) / 2f, 28.dp.toPx())
                // cut-out (a circle for square targets, rounded box for wide ones)
                drawRoundRect(Color.Black, Offset(hl, ht), Size(w, h), CornerRadius(cr), blendMode = BlendMode.Clear)
                drawRoundRect(Color.White.copy(alpha = show), Offset(hl, ht), Size(w, h), CornerRadius(cr), style = Stroke(3.dp.toPx()))
                val grow = pulse * 10.dp.toPx()
                drawRoundRect(
                    Cyan.copy(alpha = (1f - pulse) * show), Offset(hl - grow, ht - grow),
                    Size(w + grow * 2, h + grow * 2), CornerRadius(cr + grow), style = Stroke(2.5.dp.toPx())
                )
                // arrow from the card to the target
                val cx = ((hl + hr) / 2f).coerceIn(40.dp.toPx(), size.width - 40.dp.toPx())
                val dir = if (above) 1f else -1f
                val from = if (above) cardY + ch else cardY
                val edge = if (above) ht - 6.dp.toPx() else hb + 6.dp.toPx()
                val tip = edge - dir * (4.dp.toPx() + (1f - pulse) * 8.dp.toPx())
                val baseY = tip - dir * 16.dp.toPx()
                if ((baseY - from) * dir > 4.dp.toPx()) {
                    drawLine(Color.White, Offset(cx, from), Offset(cx, baseY), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
                }
                val head = Path().apply {
                    moveTo(cx, tip)
                    lineTo(cx - 11.dp.toPx(), baseY)
                    lineTo(cx + 11.dp.toPx(), baseY)
                    close()
                }
                drawPath(head, Color.White)
            }
        }

        Column(
            Modifier
                .offset { IntOffset(0, cardY.roundToInt()) }
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .onSizeChanged { cardSize = it }
                .graphicsLayer { alpha = if (cardSize.height > 0 && screen.height > 0) 1f else 0f }
                .clip(RoundedCornerShape(24.dp))
                .background(if (hs.dark) Color(0xFF111833) else Color.White)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.tourStepOf(index + 1, steps.size), color = hs.sub, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(s.tourSkip, color = hs.sub, fontSize = 13.sp, modifier = Modifier.pressable(onClick = onFinish).padding(6.dp))
            }
            Text(step.title, color = hs.text, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
            Text(step.body, color = hs.sub, fontSize = 14.sp, lineHeight = 22.sp)
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (index > 0) CoachBtn(s.tourPrev, false, Modifier.weight(1f)) { index-- }
                val last = index == steps.lastIndex
                CoachBtn(if (last) s.tourDoneBtn else s.tourNext, true, Modifier.weight(1f)) {
                    if (last) onFinish() else index++
                }
            }
        }
    }
}

@Composable
private fun CoachBtn(text: String, filled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val hs = LocalHs.current
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier.height(46.dp).pressable(onClick = onClick).clip(shape)
            .then(if (filled) Modifier.background(hs.brand) else Modifier.background(hs.card).border(1.dp, hs.cardStroke, shape)),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (filled) Color.White else hs.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
