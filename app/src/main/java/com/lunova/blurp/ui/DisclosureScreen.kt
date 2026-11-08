package com.lunova.blurp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lunova.blurp.R

/** Google Play's prominent disclosure for the Accessibility service. Nothing else belongs on this screen. */
@Composable
fun DisclosureScreen(onConsent: () -> Unit) {
    var checked by rememberSaveable { mutableStateOf(false) }
    Title(stringResource(R.string.disclosure_title))
    Body(stringResource(R.string.disclosure_body))
    Row(
        modifier = Modifier.toggleable(value = checked, role = Role.Checkbox, onValueChange = { checked = it }),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(checkedColor = Paper, uncheckedColor = Paper, checkmarkColor = Ink),
        )
        Text(stringResource(R.string.disclosure_check), color = Paper, fontSize = 15.sp, lineHeight = 21.sp)
    }
    PillButton(stringResource(R.string.disclosure_continue), enabled = checked, onClick = onConsent)
}
