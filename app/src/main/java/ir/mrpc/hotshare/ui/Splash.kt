package ir.mrpc.hotshare.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Rotating gradient arcs + dashed rings: the circular animation used around names. */
@Composable
fun OrbitRings(modifier: Modifier = Modifier, speed: Float = 1f) {
    val t = rememberInfiniteTransition(label = "orbit")
    val r1 by t.animateFloat(0f, 360f, infiniteRepeatable(tween((2600 / speed).toInt(), easing = LinearEasing)), label = "r1")
    val r2 by t.animateFloat(360f, 0f, infiniteRepeatable(tween((4200 / speed).toInt(), easing = LinearEasing)), label = "r2")
    val r3 by t.animateFloat(0f, 360f, infiniteRepeatable(tween((7000 / speed).toInt(), easing = LinearEasing)), label = "r3")
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height / 2)
        val R = size.minDimension / 2f
        fun ring(frac: Float, rot: Float, sweep: Float, w: Float, colors: List<Color>) {
            val d = R * frac * 2
            rotate(rot, c) {
                drawArc(
                    Brush.sweepGradient(colors, c), 0f, sweep, false,
                    Offset(c.x - d / 2, c.y - d / 2), Size(d, d), style = Stroke(w, cap = StrokeCap.Round)
                )
            }
        }
        ring(0.98f, r1, 120f, 7f, listOf(Color.Transparent, Cyan, Violet, Color.Transparent))
        ring(0.80f, r2, 200f, 5f, listOf(Color.Transparent, Pink, Violet, Color.Transparent))
        ring(0.62f, r3, 90f, 4f, listOf(Color.Transparent, Mint, Cyan, Color.Transparent))
        drawCircle(Color.White.copy(alpha = 0.06f), R * 0.98f, c, style = Stroke(1.5f))
        drawCircle(Color.White.copy(alpha = 0.05f), R * 0.80f, c, style = Stroke(1.5f))
        drawCircle(Color.White.copy(alpha = 0.04f), R * 0.62f, c, style = Stroke(1.5f))
    }
}

@Composable
fun Splash(onDone: () -> Unit) {
    val hs = LocalHs.current
    val name = "HotShare"
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(1900, easing = LinearEasing))
        delay(500)
        onDone()
    }
    val scale by animateFloatAsState(1f, spring(0.5f, 120f), label = "splashScale")
    AuroraBackground {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            PulseRings(Modifier.size(360.dp), Violet)
            OrbitRings(Modifier.size(300.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row {
                    name.forEachIndexed { i, ch ->
                        val a = (progress.value * name.length * 1.35f - i).coerceIn(0f, 1f)
                        Text(
                            ch.toString(), color = hs.text, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.graphicsLayer { alpha = a; translationY = (1f - a) * 30f; scaleX = scale; scaleY = scale }
                        )
                    }
                }
            }
        }
        val tag = LocalStrings.current.designedBy
        val a2 = ((progress.value - 0.6f) / 0.4f).coerceIn(0f, 1f)
        Box(Modifier.fillMaxSize().padding(bottom = 48.dp), contentAlignment = Alignment.BottomCenter) {
            Text(tag, color = hs.sub, fontSize = 13.sp, modifier = Modifier.graphicsLayer { alpha = a2 })
        }
    }
}
