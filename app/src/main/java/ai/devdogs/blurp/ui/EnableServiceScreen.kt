package ai.devdogs.blurp.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ai.devdogs.blurp.R

@Composable
fun EnableServiceScreen(onOpenSettings: () -> Unit) {
    Title(stringResource(R.string.enable_title))
    Body(stringResource(R.string.enable_body))
    PillButton(stringResource(R.string.enable_button), onClick = onOpenSettings)
}
