package ai.devdogs.blurp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ai.devdogs.blurp.R

/**
 * Sends people to Settings to switch Blurp on. Installed from a downloaded file, Blurp is held
 * back there until restricted settings are allowed in its App info, so [restrictedHelp] puts that
 * step first, in the words One UI 9 uses ("App was denied access").
 */
@Composable
fun EnableServiceScreen(onOpenSettings: () -> Unit, restrictedHelp: Boolean, onOpenAppInfo: () -> Unit) {
    Title(stringResource(R.string.enable_title))
    if (!restrictedHelp) {
        Body(stringResource(R.string.enable_body))
        PillButton(stringResource(R.string.enable_button), onClick = onOpenSettings)
        return
    }
    Body(stringResource(R.string.enable_restricted_body))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PillButton(stringResource(R.string.enable_app_info), onClick = onOpenAppInfo)
        GhostButton(stringResource(R.string.enable_button), onClick = onOpenSettings)
    }
}
