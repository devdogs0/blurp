package ai.devdogs.blurp.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.devdogs.blurp.HingeController
import ai.devdogs.blurp.R

/** Finishes by itself once the phone has been folded and opened again. */
@Composable
fun TutorialScreen(onDone: () -> Unit) {
    val controller = HingeController.get(LocalContext.current)
    val start = remember { controller.foldsCompleted }
    LaunchedEffect(controller.foldsCompleted) {
        if (controller.foldsCompleted > start) onDone()
    }
    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
        FoldingPhone()
    }
    Title(stringResource(R.string.tutorial_title))
    Body(stringResource(R.string.tutorial_body))
    TextButton(onClick = onDone) {
        Text(stringResource(R.string.tutorial_done), color = Muted, fontSize = 16.sp)
    }
}

@Composable
private fun FoldingPhone() {
    val swing by rememberInfiniteTransition(label = "fold").animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2600
                0f at 400 using FastOutSlowInEasing
                170f at 1300
                170f at 1700 using FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "swing",
    )
    val half = Modifier
        .size(width = 64.dp, height = 92.dp)
        .background(Glow, RoundedCornerShape(10.dp))
        .border(2.dp, Paper, RoundedCornerShape(10.dp))
    Row {
        Box(
            half.graphicsLayer {
                transformOrigin = TransformOrigin(1f, 0.5f)
                rotationY = swing
                cameraDistance = 12f * density
            },
        )
        Box(half)
    }
}
