package ir.mrpc.hotshare.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.mrpc.hotshare.core.Prefs

private class ObPage(val icon: ImageVector, val color: Color, val title: String, val body: String)

/**
 * The one and only tutorial: a single short walkthrough for the whole app.
 * Shown automatically on the very first launch; afterwards only when the user turns it on
 * in Menu -> General (or opens it from there / from Help).
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val s = LocalStrings.current
    val hs = LocalHs.current
    val pages = remember(s) {
        listOf(
            ObPage(Icons.Rounded.Shield, Cyan, s.ob1Title, s.ob1Body),
            ObPage(Icons.Rounded.Share, Violet, s.ob2Title, s.ob2Body),
            ObPage(Icons.Rounded.Download, Pink, s.ob3Title, s.ob3Body),
            ObPage(Icons.Rounded.Wifi, Amber, s.ob4Title, s.ob4Body),
            ObPage(Icons.Rounded.CheckCircle, Mint, s.ob5Title, s.ob5Body)
        )
    }
    var index by remember { mutableIntStateOf(0) }
    val last = index == pages.lastIndex

    // seeing it once is enough for the automatic first-launch showing
    LaunchedEffect(Unit) { if (!Prefs.tutorialSeen) Prefs.markTutorialSeen() }
    BackHandler(enabled = index > 0) { index-- }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(s.obPage(index + 1, pages.size), color = hs.sub, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (!last) {
                Text(s.obSkip, color = hs.sub, fontSize = 14.sp, modifier = Modifier.pressable(onClick = onFinish).padding(8.dp))
            }
        }

        AnimatedContent(
            targetState = index,
            transitionSpec = {
                if (targetState > initialState)
                    (slideInHorizontally(tween(350)) { it / 3 } + fadeIn(tween(350))) togetherWith (slideOutHorizontally(tween(300)) { -it / 3 } + fadeOut(tween(200)))
                else
                    (slideInHorizontally(tween(350)) { -it / 3 } + fadeIn(tween(350))) togetherWith (slideOutHorizontally(tween(300)) { it / 3 } + fadeOut(tween(200)))
            },
            modifier = Modifier.weight(1f).fillMaxWidth(), label = "onboard"
        ) { i ->
            val p = pages[i]
            Column(
                Modifier.fillMaxSize().padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically)
            ) {
                Box(Modifier.size(230.dp), contentAlignment = Alignment.Center) {
                    PulseRings(Modifier.fillMaxSize(), p.color)
                    OrbitRings(Modifier.size(172.dp), speed = 0.8f)
                    Box(
                        Modifier.size(100.dp).clip(CircleShape).background(Brush.linearGradient(listOf(p.color, p.color.copy(alpha = 0.55f)))),
                        contentAlignment = Alignment.Center
                    ) { Icon(p.icon, null, tint = Color.White, modifier = Modifier.size(50.dp)) }
                }
                Text(p.title, color = hs.text, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 33.sp)
                Text(p.body, color = hs.sub, fontSize = 15.sp, lineHeight = 25.sp, textAlign = TextAlign.Center)
            }
        }

        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 20.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                pages.forEachIndexed { i, p ->
                    val w by animateDpAsState(if (i == index) 26.dp else 8.dp, tween(250), label = "dot")
                    Box(Modifier.width(w).height(8.dp).clip(CircleShape).background(if (i == index) p.color else hs.cardStroke))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (index > 0) GhostButton(s.obPrev, { index-- }, Modifier.weight(1f))
                GradientButton(if (last) s.obDone else s.obNext, { if (last) onFinish() else index++ }, Modifier.weight(if (index > 0) 1.6f else 1f))
            }
        }
    }
}
