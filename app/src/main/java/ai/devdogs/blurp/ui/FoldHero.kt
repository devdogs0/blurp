package ai.devdogs.blurp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ai.devdogs.blurp.R

/** The inner screen's shape: 2448 x 1848 on a Galaxy Z Fold8. */
private const val INNER_SCREEN_RATIO = 2448f / 1848f

private val frame = RoundedCornerShape(26.dp)
private val screen = RoundedCornerShape(17.dp)

/**
 * An unfolded Galaxy Z Fold showing its lock screen mid-fold, the moving half frosted: a real
 * capture of Blurp at work, framed like the card on devdogs.ai.
 */
@Composable
fun FoldHero(modifier: Modifier = Modifier) {
    Box(
        modifier
            .aspectRatio(INNER_SCREEN_RATIO)
            .shadow(36.dp, frame, ambientColor = Accent.copy(alpha = 0.35f), spotColor = Accent.copy(alpha = 0.6f))
            .clip(frame)
            .background(Brush.verticalGradient(listOf(Color(0xFF1B1B22), Color(0xFF09090C))))
            .border(1.dp, Color.White.copy(alpha = 0.1f), frame)
            .padding(10.dp),
    ) {
        Box(Modifier.fillMaxSize().clip(screen)) {
            Image(
                painter = painterResource(R.drawable.welcome_frost),
                contentDescription = stringResource(R.string.hero_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // The crease down the middle of the unfolded screen.
            Box(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxHeight()
                    .width(12.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.05f),
                                Color.Black.copy(alpha = 0.28f),
                                Color.White.copy(alpha = 0.05f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
        }
    }
}
