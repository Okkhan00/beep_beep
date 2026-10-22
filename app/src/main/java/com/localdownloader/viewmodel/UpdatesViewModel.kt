package com.localdownloader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localdownloader.data.DownloadTaskStore
import com.localdownloader.domain.models.blocksRuntimeUpdates
import com.localdownloader.updates.ComponentUpdateCheck
import com.localdownloader.updates.StartupUpdatePrompt
import com.localdownloader.updates.UpdatePreferences
import com.localdownloader.updates.UpdatePreferencesStore
import com.localdownloader.updates.YtDlpReleaseChannel
import com.localdownloader.updates.YtDlpUpdateManager
import com.localdownloader.worker.YtDlpUpdateScheduler
import com.localdownloader.worker.YtDlpUpdateStateStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Manages the yt-dlp runtime updater only. The app self-updater and the FFmpeg overlay updater
 * were removed; the app is updated through the distribution channel the user installed it from.
 */
@HiltViewModel
class UpdatesViewModel @Inject constructor(
    private val ytDlpUpdateManager: YtDlpUpdateManager,
    private val updatePreferencesStore: UpdatePreferencesStore,
    private val downloadTaskStore: DownloadTaskStore,
    private val ytDlpUpdateScheduler: YtDlpUpdateScheduler,
    private val ytDlpUpdateStateStore: YtDlpUpdateStateStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UpdatesUiState())
    val uiState = _uiState.asStateFlow()

    fun initialize() {
        _uiState.value = _uiState.value.copy(preferences = updatePreferencesStore.currentPreferences())
        refreshAll()
    }

    fun checkForStartupUpdates(force: Boolean = false) {
        val preferences = updatePreferencesStore.currentPreferences()
        _uiState.value = _uiState.value.copy(preferences = preferences)
        if (!preferences.checkUpdatesOnStartup && !force) return

        val now = System.currentTimeMillis()
        val lastCheck = updatePreferencesStore.getLastStartupCheckEpochMs()
        if (!force && (now - lastCheck) < STARTUP_CHECK_COOLDOWN_MS) return

        viewModelScope.launch {
            val ytDlpCheck = runCatching { ytDlpUpdateManager.check(preferences.ytDlpChannel) }.getOrNull()
            updatePreferencesStore.setLastStartupCheckEpochMs(now)

            _uiState.value = _uiState.value.copy(
                ytDlp = ytDlpCheck?.let {
                    _uiState.value.ytDlp.fromCheck(it).copy(lastStatus = ytDlpUpdateStateStore.lastStatus())
                } ?: _uiState.value.ytDlp,
            )

            val dismissedYtDlp = updatePreferencesStore.getDismissedYtDlpVersion()
            val validYtDlpUpdate = ytDlpCheck?.takeIf { it.updateAvailable && it.latestVersion != dismissedYtDlp }
            if (validYtDlpUpdate != null) {
                _uiState.value = _uiState.value.copy(
                    startupUpdatePrompt = StartupUpdatePrompt(ytDlpUpdate = validYtDlpUpdate),
                )
            }
        }
    }

    fun dismissStartupUpdatePrompt(snoozeCurrentVersions: Boolean = false) {
        val prompt = _uiState.value.startupUpdatePrompt
        if (snoozeCurrentVersions && prompt != null) {
            prompt.ytDlpUpdate?.latestVersion?.let { updatePreferencesStore.setDismissedYtDlpVersion(it) }
        }
        _uiState.value = _uiState.value.copy(startupUpdatePrompt = null)
    }

    fun setCheckUpdatesOnStartup(enabled: Boolean) {
        updatePreferencesStore.setCheckUpdatesOnStartup(enabled)
        _uiState.value = _uiState.value.copy(
            preferences = _uiState.value.preferences.copy(checkUpdatesOnStartup = enabled),
        )
    }

    fun refreshAll() {
        refreshYtDlp()
    }

    fun refreshYtDlp() {
        viewModelScope.launch { refreshYtDlpInternal() }
    }

    fun setAutoUpdateYtDlp(enabled: Boolean) {
        updatePreferencesStore.setAutoUpdateYtDlp(enabled)
        _uiState.value = _uiState.value.copy(
            preferences = _uiState.value.preferences.copy(autoUpdateYtDlp = enabled),
        )
        ytDlpUpdateScheduler.cancelScheduled()
        if (enabled) {
            ytDlpUpdateScheduler.scheduleIfDue()
        }
    }

    fun setYtDlpChannel(channel: YtDlpReleaseChannel) {
        updatePreferencesStore.setYtDlpChannel(channel)
        _uiState.value = _uiState.value.copy(
            preferences = _uiState.value.preferences.copy(ytDlpChannel = channel),
        )
        if (_uiState.value.preferences.autoUpdateYtDlp) {
            ytDlpUpdateScheduler.cancelScheduled()
            ytDlpUpdateScheduler.scheduleIfDue()
        }
        refreshYtDlp()
    }

    fun installYtDlpUpdate() {
        val channel = _uiState.value.preferences.ytDlpChannel
        viewModelScope.launch {
            if (hasBlockingDownloads()) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Finish, cancel, or resume queued downloads before updating yt-dlp.",
                    infoMessage = null,
                )
                return@launch
            }
            ytDlpUpdateStateStore.markAttemptStarted()
            _uiState.value = _uiState.value.copy(
                ytDlp = _uiState.value.ytDlp.copy(isInstalling = true, progressPercent = 0),
                infoMessage = null,
                errorMessage = null,
            )
            runCatching {
                ytDlpUpdateManager.installUpdate(channel) { progress ->
                    _uiState.value = _uiState.value.copy(
                        ytDlp = _uiState.value.ytDlp.copy(progressPercent = progress),
                    )
                }
            }.onSuccess { result ->
                ytDlpUpdateStateStore.markAttemptSucceeded(result.message)
                _uiState.value = _uiState.value.copy(
                    ytDlp = _uiState.value.ytDlp.copy(isInstalling = false, progressPercent = null),
                    infoMessage = result.message,
                )
                refreshYtDlp()
            }.onFailure { error ->
                ytDlpUpdateStateStore.markAttemptFailed(error.message ?: "manual_update_failed")
                _uiState.value = _uiState.value.copy(
                    ytDlp = _uiState.value.ytDlp.copy(isInstalling = false, progressPercent = null),
                    errorMessage = error.message ?: "Failed to update yt-dlp.",
                )
            }
        }
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(infoMessage = null, errorMessage = null)
    }

    private suspend fun refreshYtDlpInternal() {
        _uiState.value = _uiState.value.copy(ytDlp = _uiState.value.ytDlp.copy(isChecking = true))
        runCatching {
            ytDlpUpdateManager.check(_uiState.value.preferences.ytDlpChannel)
        }.onSuccess { check ->
            _uiState.value = _uiState.value.copy(
                ytDlp = _uiState.value.ytDlp.fromCheck(check).copy(
                    isChecking = false,
                    lastStatus = ytDlpUpdateStateStore.lastStatus(),
                ),
            )
        }.onFailure { error ->
            _uiState.value = _uiState.value.copy(
                ytDlp = _uiState.value.ytDlp.copy(
                    isChecking = false,
                    summary = error.message ?: "Failed to check yt-dlp updates.",
                    lastStatus = ytDlpUpdateStateStore.lastStatus(),
                ),
            )
        }
    }

    private fun hasBlockingDownloads(): Boolean {
        return downloadTaskStore.getAllTasks().any { task -> task.status.blocksRuntimeUpdates() }
    }

    private companion object {
        private const val STARTUP_CHECK_COOLDOWN_MS = 12 * 60 * 60 * 1000L // 12 hours
    }
}

data class UpdatesUiState(
    val preferences: UpdatePreferences = UpdatePreferences(),
    val ytDlp: UpdateSectionUiState = UpdateSectionUiState(
        title = "yt-dlp update",
        subtitle = "Switch sources and install newer downloader builds when extractor changes are needed.",
    ),
    val startupUpdatePrompt: StartupUpdatePrompt? = null,
    val infoMessage: String? = null,
    val errorMessage: String? = null,
)

data class UpdateSectionUiState(
    val title: String,
    val subtitle: String,
    val currentVersion: String? = null,
    val latestVersion: String? = null,
    val summary: String = "Not checked yet.",
    val releaseNotes: String? = null,
    val releasePageUrl: String? = null,
    val isChecking: Boolean = false,
    val isInstalling: Boolean = false,
    val updateAvailable: Boolean = false,
    val progressPercent: Int? = null,
    val latestCheck: ComponentUpdateCheck? = null,
    val lastStatus: String? = null,
)

private fun UpdateSectionUiState.fromCheck(check: ComponentUpdateCheck): UpdateSectionUiState {
    return copy(
        currentVersion = check.currentVersion,
        latestVersion = check.latestVersion,
        summary = check.summary,
        releaseNotes = check.releaseNotes,
        releasePageUrl = check.releasePageUrl,
        updateAvailable = check.updateAvailable,
        latestCheck = check,
    )
}
