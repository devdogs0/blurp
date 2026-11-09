package ai.devdogs.blurp.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.devdogs.blurp.Screen

internal val Ink = Color(0xFF0B0A10)
internal val Glow = Color(0xFF3A2A55)
internal val Paper = Color(0xFFF2F0F7)
internal val Muted = Paper.copy(alpha = 0.72f)
internal val Accent = Color(0xFF7C5CFF)

class BlurpActions(
    val onConsent: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onTutorialDone: () -> Unit,
    val onFrostChange: (Boolean) -> Unit,
)

@Composable
fun BlurpApp(screen: Screen, serviceEnabled: Boolean, frostEnabled: Boolean, actions: BlurpActions) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = Ink, surface = Ink)) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(Ink)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Glow, Ink.copy(alpha = 0f)),
                        center = Offset(Float.POSITIVE_INFINITY, 0f),
                        radius = 1600f,
                    ),
                ),
        ) {
            val wide = maxWidth >= 600.dp
            AnimatedContent(
                targetState = screen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen",
            ) { target ->
                ScreenColumn(wide) {
                    when (target) {
                        Screen.Unsupported -> UnsupportedScreen()
                        Screen.Disclosure -> DisclosureScreen(actions.onConsent)
                        Screen.EnableService -> EnableServiceScreen(actions.onOpenSettings)
                        Screen.Tutorial -> TutorialScreen(actions.onTutorialDone)
                        Screen.Home -> HomeScreen(serviceEnabled, frostEnabled, actions.onFrostChange, actions.onOpenSettings)
                    }
                }
            }
        }
    }
}

/** One centered column: at most 480 dp wide on the inner screen, edge to edge on the cover. */
@Composable
private fun ScreenColumn(wide: Boolean, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .then(if (wide) Modifier.widthIn(max = 480.dp) else Modifier.fillMaxWidth())
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

@Composable
internal fun Title(text: String) {
    Text(text, color = Paper, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, lineHeight = 36.sp)
}

@Composable
internal fun Body(text: String) {
    Text(text, color = Muted, fontSize = 17.sp, lineHeight = 25.sp)
}

@Composable
internal fun PillButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(percent = 50),
        colors = ButtonDefaults.buttonColors(
            containerColor = Paper,
            contentColor = Ink,
            disabledContainerColor = Paper.copy(alpha = 0.2f),
            disabledContentColor = Paper.copy(alpha = 0.5f),
        ),
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
    }
}
