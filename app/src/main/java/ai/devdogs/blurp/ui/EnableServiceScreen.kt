package ai.devdogs.blurp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ai.devdogs.blurp.R
import ai.devdogs.blurp.RestrictedSettingsStep

/**
 * Sends people to Settings to switch Blurp on. Installed from a downloaded file, Blurp is refused
 * there once ("App was denied access"); only after that does App info's ⋮ menu offer "Allow
 * restricted settings", so [step] says what to do now: try the switch, or allow it in App info.
 */
@Composable
fun EnableServiceScreen(step: RestrictedSettingsStep, onOpenSettings: () -> Unit, onOpenAppInfo: () -> Unit) {
    Title(stringResource(R.string.enable_title))
    when (step) {
        RestrictedSettingsStep.None -> {
            Body(stringResource(R.string.enable_body))
            PillButton(stringResource(R.string.enable_button), onClick = onOpenSettings)
        }
        RestrictedSettingsStep.TryFirst -> {
            Body(stringResource(R.string.enable_try_first))
            PillButton(stringResource(R.string.enable_button), onClick = onOpenSettings)
        }
        RestrictedSettingsStep.AllowNow -> {
            Body(stringResource(R.string.enable_allow_now))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PillButton(stringResource(R.string.enable_app_info), onClick = onOpenAppInfo)
                GhostButton(stringResource(R.string.enable_button), onClick = onOpenSettings)
            }
        }
    }
}
