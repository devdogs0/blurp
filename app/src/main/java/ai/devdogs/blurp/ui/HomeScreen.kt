package ai.devdogs.blurp.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import ai.devdogs.blurp.R

@Composable
fun HomeScreen(
    serviceEnabled: Boolean,
    frostEnabled: Boolean,
    onFrostChange: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Title(stringResource(R.string.app_name))
    Body(stringResource(R.string.home_intro))
    if (!serviceEnabled) {
        Body(stringResource(R.string.home_service_off))
        PillButton(stringResource(R.string.home_turn_on), onClick = onOpenSettings)
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = frostEnabled, role = Role.Switch, onValueChange = onFrostChange),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.home_switch), color = Paper, fontSize = 18.sp)
        Switch(
            checked = frostEnabled,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedTrackColor = Accent, checkedThumbColor = Paper),
        )
    }
    Body(stringResource(if (frostEnabled) R.string.home_on else R.string.home_off))
}
