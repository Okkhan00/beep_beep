package com.localdownloader.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.localdownloader.BuildConfig
import com.localdownloader.R
import com.localdownloader.ui.components.InlineFeedbackCard
import com.localdownloader.ui.components.PreferenceItem
import com.localdownloader.ui.components.PreferenceNavigationRow
import com.localdownloader.ui.components.PreferencePageScaffold
import com.localdownloader.ui.components.PreferenceSubtitle
import com.localdownloader.ui.components.PreferenceSwitch
import com.localdownloader.ui.screens.settings.SettingChoiceDialog
import com.localdownloader.ui.screens.settings.SettingChoiceDialogState
import com.localdownloader.ui.screens.settings.SettingChoiceOption
import com.localdownloader.updates.YtDlpReleaseChannel
import com.localdownloader.viewmodel.UpdateSectionUiState
import com.localdownloader.viewmodel.UpdatesUiState

@Composable
fun UpdatesScreen(
    uiState: UpdatesUiState,
    onBack: () -> Unit,
    onRefreshAll: () -> Unit,
    onRefreshYtDlp: () -> Unit,
    onInstallYtDlpUpdate: () -> Unit,
    onYtDlpChannelChanged: (YtDlpReleaseChannel) -> Unit,
    onAutoUpdateYtDlpChanged: (Boolean) -> Unit,
    onCheckUpdatesOnStartupChanged: (Boolean) -> Unit,
    onOpenChangelog: (String) -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var choiceDialog by remember { mutableStateOf<SettingChoiceDialogState?>(null) }

    choiceDialog?.let { state ->
        SettingChoiceDialog(
            state = state,
            onDismiss = { choiceDialog = null },
        )
    }

    PreferencePageScaffold(
        title = stringResource(R.string.updates_title),
        onBack = onBack,
        modifier = modifier,
        actions = {
            IconButton(onClick = onRefreshAll) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.updates_refresh_all),
                )
            }
        },
    ) {
        if (uiState.infoMessage != null || uiState.errorMessage != null) {
            item {
                InlineFeedbackCard(
                    label = "Updates",
                    message = uiState.errorMessage ?: uiState.infoMessage.orEmpty(),
                    isError = uiState.errorMessage != null,
                    onDismiss = onDismissMessage,
                )
            }
        }

        item {
            PreferenceItem(
                icon = Icons.Rounded.Info,
                title = stringResource(R.string.updates_current_version),
                description = "App version. The app itself is updated through the source you installed it from.",
                value = BuildConfig.VERSION_NAME,
                onClick = null,
            )
        }
        item {
            PreferenceSwitch(
                icon = Icons.Rounded.Sync,
                title = stringResource(R.string.updates_check_on_startup_title),
                description = stringResource(R.string.updates_check_on_startup_subtitle),
                isChecked = uiState.preferences.checkUpdatesOnStartup,
                onClick = { onCheckUpdatesOnStartupChanged(!uiState.preferences.checkUpdatesOnStartup) },
            )
        }

        item {
            PreferenceSubtitle(text = "YT-DLP RUNTIME")
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.Code,
                title = stringResource(R.string.updates_current_version),
                description = stringResource(R.string.updates_ytdlp_current_subtitle),
                value = uiState.ytDlp.currentVersion ?: stringResource(R.string.common_unknown),
                onClick = null,
            )
        }
        item {
            val ytdlpSourceTitle = stringResource(R.string.updates_ytdlp_source)
            PreferenceItem(
                icon = Icons.Rounded.Settings,
                title = ytdlpSourceTitle,
                description = uiState.preferences.ytDlpChannel.description,
                value = uiState.preferences.ytDlpChannel.id,
                onClick = {
                    choiceDialog = SettingChoiceDialogState(
                        title = ytdlpSourceTitle,
                        selected = uiState.preferences.ytDlpChannel.title,
                        options = YtDlpReleaseChannel.entries.map { channel ->
                            SettingChoiceOption(
                                title = channel.title,
                                subtitle = channel.description,
                                onSelect = { onYtDlpChannelChanged(channel) },
                            )
                        },
                    )
                },
            )
        }
        item {
            PreferenceSwitch(
                icon = Icons.Rounded.Sync,
                title = stringResource(R.string.updates_auto_update_ytdlp),
                description = stringResource(
                    if (BuildConfig.YTDLP_AUTO_UPDATE_DEFAULT) {
                        R.string.updates_ytdlp_auto_subtitle
                    } else {
                        R.string.updates_ytdlp_auto_subtitle_repo_safe
                    },
                ),
                isChecked = uiState.preferences.autoUpdateYtDlp,
                onClick = { onAutoUpdateYtDlpChanged(!uiState.preferences.autoUpdateYtDlp) },
            )
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.Refresh,
                title = stringResource(R.string.updates_check_now),
                description = buildCheckSubtitle(uiState.ytDlp),
                onClick = onRefreshYtDlp,
            )
        }
        if (uiState.ytDlp.updateAvailable || uiState.ytDlp.isInstalling) {
            item {
                PreferenceItem(
                    icon = Icons.Rounded.Download,
                    title = stringResource(R.string.updates_install_ytdlp),
                    description = buildInstallSubtitle(section = uiState.ytDlp),
                    onClick = onInstallYtDlpUpdate,
                )
            }
        }
        item {
            PreferenceNavigationRow(
                icon = Icons.Rounded.Description,
                title = stringResource(R.string.updates_changelog),
                description = if (uiState.ytDlp.releaseNotes.isNullOrBlank()) {
                    stringResource(R.string.updates_ytdlp_changelog_subtitle_fallback)
                } else {
                    stringResource(R.string.updates_ytdlp_changelog_subtitle_ready)
                },
                onClick = { onOpenChangelog(UpdateChangelogSections.YT_DLP) },
            )
        }

    }
}

private fun buildCheckSubtitle(section: UpdateSectionUiState): String {
    return when {
        section.isChecking -> "Checking for updates..."
        section.updateAvailable -> "Latest available: ${section.latestVersion ?: "Unknown"}"
        !section.lastStatus.isNullOrBlank() -> "${section.summary} Last status: ${section.lastStatus}"
        else -> section.summary
    }
}

private fun buildInstallSubtitle(section: UpdateSectionUiState): String {
    return when {
        section.isInstalling && section.progressPercent != null -> "Downloading... ${section.progressPercent}%"
        section.updateAvailable -> "Install version ${section.latestVersion ?: "the latest version"}"
        else -> section.summary
    }
}
