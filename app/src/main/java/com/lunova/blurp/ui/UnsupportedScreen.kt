package com.lunova.blurp.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lunova.blurp.R

@Composable
fun UnsupportedScreen() {
    Title(stringResource(R.string.unsupported_title))
    Body(stringResource(R.string.unsupported_body))
}
