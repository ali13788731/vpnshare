package ir.mrpc.hotshare.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onDone: () -> Unit) {
    val s = LocalStrings.current
    val logo = remember { Animatable(0f) }
    val scale = remember { Animatable(.6f) }
    val titleAlpha = remember { Animatable(0f) }
    var typed by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = .5f, stiffness = 120f))
    }
    LaunchedEffect(Unit) {
        logo.animateTo(1f, tween(1500, easing = FastOutSlowInEasing))
        titleAlpha.animateTo(1f, tween(500))
        val text = s.designedBy
        for (i in 1..text.length) { typed = i; delay(55) }
        delay(800)
        onDone()
    }

    AuroraBackground {
        Column(Modifier.fillMaxSize().systemBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            Logo(Modifier.size(150.dp).scale(scale.value), logo.value)
            Spacer(Modifier.height(28.dp))
            Text(
                s.appName, Modifier.alpha(titleAlpha.value),
                style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold
            )
            Text(s.tagline, Modifier.alpha(titleAlpha.value).padding(top = 6.dp, start = 24.dp, end = 24.dp),
                fontSize = 14.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .65f))
            Spacer(Modifier.weight(1f))
            Text(
                s.designedBy.take(typed),
                Modifier.padding(bottom = 36.dp).heightIn(min = 24.dp),
                fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Cyan
            )
        }
    }
}
