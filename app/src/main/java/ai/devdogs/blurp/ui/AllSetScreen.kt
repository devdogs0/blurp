package ai.devdogs.blurp.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ai.devdogs.blurp.R

/** After the first fold: setup is over. Shown once, wherever the fold happened. */
@Composable
fun AllSetScreen(onDone: () -> Unit) {
    Title(stringResource(R.string.allset_title))
    Body(stringResource(R.string.allset_body))
    PillButton(stringResource(R.string.allset_done), onClick = onDone)
}
