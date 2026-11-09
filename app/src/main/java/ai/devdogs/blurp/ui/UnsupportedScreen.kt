package ai.devdogs.blurp.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ai.devdogs.blurp.R

@Composable
fun UnsupportedScreen() {
    Title(stringResource(R.string.unsupported_title))
    Body(stringResource(R.string.unsupported_body))
}
