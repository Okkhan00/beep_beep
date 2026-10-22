package com.localdownloader.updates

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpdateModelsTest {

    @Test
    fun startupUpdatePrompt_hasAnyUpdate_reflectsYtDlpUpdate() {
        assertFalse(StartupUpdatePrompt().hasAnyUpdate)
        val none = StartupUpdatePrompt(
            ytDlpUpdate = ComponentUpdateCheck(
                currentVersion = "2026.01.01",
                latestVersion = "2026.01.01",
                updateAvailable = false,
                summary = "Up to date",
            ),
        )
        assertFalse(none.hasAnyUpdate)
        val available = StartupUpdatePrompt(
            ytDlpUpdate = ComponentUpdateCheck(
                currentVersion = "2026.01.01",
                latestVersion = "2026.02.01",
                updateAvailable = true,
                summary = "Update available",
            ),
        )
        assertTrue(available.hasAnyUpdate)
    }

    @Test
    fun updatePreferences_defaultCheckUpdatesOnStartup_isTrue() {
        val prefs = UpdatePreferences()
        assertTrue(prefs.checkUpdatesOnStartup)
    }

    @Test
    fun compareLooseVersions_comparesSemVerAndDateVersionsCorrectly() {
        assertTrue(compareLooseVersions("1.0.0", "1.0.1") < 0)
        assertTrue(compareLooseVersions("1.1.0", "1.0.1") > 0)
        assertTrue(compareLooseVersions("2.0.0", "2.0.0") == 0)
        assertTrue(compareLooseVersions("2026.01.01", "2026.02.01") < 0)
        assertTrue(compareLooseVersions("v2.1.0", "2.1.0") == 0)
        assertTrue(compareLooseVersions("7.1.1", "7.1.2") < 0)
        assertTrue(compareLooseVersions("7.1.2", "7.1.1") > 0)
        assertTrue(compareLooseVersions("7.1.2", "v7.1.2") == 0)
    }
}
