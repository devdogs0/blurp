package ai.devdogs.blurp.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.devdogs.blurp.R
import ai.devdogs.blurp.RestrictedSettingsStep

/** The steps that aren't next: dimmer than the body, still above 4.5:1 on [Ink]. */
private val Dim = Paper.copy(alpha = 0.55f)

/** A part of One UI 9's Settings, as the user will find it, with the place to tap ringed. */
private class Guide(@DrawableRes val picture: Int, @StringRes val description: Int)

private val InstalledAppsGuide = Guide(R.drawable.guide_installed_apps, R.string.guide_installed_apps)
private val AllowRestrictedGuide = Guide(R.drawable.guide_allow_restricted, R.string.guide_allow_restricted)

/**
 * Sends people to Settings to switch Blurp on. Settings opens on Accessibility with Installed apps
 * highlighted; apps can't open Blurp's own page there. Installed from a downloaded file, Blurp is
 * refused there once ("App was denied access"), and only after that does App info's ⋮ menu offer
 * "Allow restricted settings". So that install sees Android's whole path at once, with [step] lit,
 * its picture under it and its button first.
 */
@Composable
fun EnableServiceScreen(step: RestrictedSettingsStep, onOpenSettings: () -> Unit, onOpenAppInfo: () -> Unit) {
    val settings = stringResource(R.string.enable_button)
    val appInfo = stringResource(R.string.enable_app_info)
    Title(stringResource(R.string.enable_title))
    if (step == RestrictedSettingsStep.None) {
        Body(stringResource(R.string.enable_body))
        GuidePicture(InstalledAppsGuide)
        PillButton(settings, onClick = onOpenSettings)
    } else {
        Body(stringResource(R.string.enable_download_intro))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SetupStep(1, stringResource(R.string.enable_step_try), step == RestrictedSettingsStep.TryFirst, InstalledAppsGuide)
            SetupStep(2, stringResource(R.string.enable_step_allow), step == RestrictedSettingsStep.AllowNow, AllowRestrictedGuide)
            SetupStep(3, stringResource(R.string.enable_step_switch_on), step == RestrictedSettingsStep.SwitchOn, InstalledAppsGuide)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            when (step) {
                RestrictedSettingsStep.AllowNow -> {
                    PillButton(appInfo, onClick = onOpenAppInfo)
                    GhostButton(settings, onClick = onOpenSettings)
                }
                RestrictedSettingsStep.SwitchOn -> {
                    PillButton(settings, onClick = onOpenSettings)
                    GhostButton(appInfo, onClick = onOpenAppInfo)
                }
                // Step 1 can only happen in Settings: App info has nothing to allow before the refusal.
                else -> PillButton(settings, onClick = onOpenSettings)
            }
        }
    }
}

/** One numbered step: the next one bright with a lit number and its picture, the others dim. */
@Composable
private fun SetupStep(number: Int, text: String, next: Boolean, guide: Guide) {
    val nextLabel = stringResource(R.string.enable_step_next)
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) { if (next) stateDescription = nextLabel },
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(if (next) Accent else Color.Transparent, CircleShape)
                .border(1.dp, if (next) Accent else Dim, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", color = if (next) Paper else Dim, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text, color = if (next) Paper else Dim, fontSize = 17.sp, lineHeight = 25.sp)
            if (next) GuidePicture(guide)
        }
    }
}

@Composable
private fun GuidePicture(guide: Guide) {
    val shape = RoundedCornerShape(12.dp)
    Image(
        painter = painterResource(guide.picture),
        contentDescription = stringResource(guide.description),
        contentScale = ContentScale.FillWidth,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, Paper.copy(alpha = 0.15f), shape),
    )
}
