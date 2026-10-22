package com.localdownloader.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.localdownloader.R
import com.localdownloader.ui.components.PreferencePageScaffold
import com.localdownloader.ui.components.PreferenceSubtitle
import com.localdownloader.ui.components.PreferenceSwitch
import com.localdownloader.viewmodel.FormatUiState

/** Network rules only. Cookie import and account/session features were removed. */
@Composable
fun AccessSettingsScreen(
    uiState: FormatUiState,
    onAllowMeteredDownloadsChanged: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PreferencePageScaffold(
        title = stringResource(R.string.settings_access_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        item {
            PreferenceSubtitle(text = "NETWORK RULES")
        }
        item {
            PreferenceSwitch(
                icon = Icons.Rounded.SignalCellularAlt,
                title = stringResource(R.string.access_download_cellular_title),
                description = stringResource(R.string.access_download_cellular_subtitle),
                isChecked = uiState.allowMeteredDownloads,
                onClick = { onAllowMeteredDownloadsChanged(!uiState.allowMeteredDownloads) },
            )
        }
    }
}
