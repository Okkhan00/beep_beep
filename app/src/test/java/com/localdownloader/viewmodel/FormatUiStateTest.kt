package com.localdownloader.viewmodel

import com.localdownloader.domain.models.PlaylistEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatUiStateTest {

    @Test
    fun scopedMessages_onlyAppearOnMatchingScreen() {
        val state = FormatUiState(
            messageScope = FormatMessageScope.SETTINGS,
            infoMessage = "Saved.",
            errorMessage = null,
        )

        assertEquals("Saved.", state.infoMessageFor(FormatMessageScope.SETTINGS))
        assertNull(state.infoMessageFor(FormatMessageScope.BROWSER))
        assertNull(state.errorMessageFor(FormatMessageScope.SETTINGS))
    }

    @Test
    fun playlistSelectionCounters_followSelectedItems() {
        val state = FormatUiState(
            playlistItems = listOf(
                PlaylistItemUiState(
                    entry = PlaylistEntry(
                        playlistItemIndex = 1,
                        id = "one",
                        title = "First",
                        webpageUrl = "https://example.com/1",
                    ),
                    isSelected = true,
                ),
                PlaylistItemUiState(
                    entry = PlaylistEntry(
                        playlistItemIndex = 2,
                        id = "two",
                        title = "Second",
                        webpageUrl = "https://example.com/2",
                    ),
                    isSelected = false,
                ),
            ),
        )

        assertEquals(1, state.selectedPlaylistItemCount)
        assertFalse(state.areAllPlaylistItemsSelected)
    }

    @Test
    fun appSettings_formatDisplayDefaults_areTrue() {
        val state = FormatUiState()
        assertTrue(state.appSettings.showFormatFps)
        assertTrue(state.appSettings.showFormatCodec)
        assertTrue(state.appSettings.showFormatBitrate)
        assertEquals(com.localdownloader.domain.models.FormatSelectorStyle.BOTTOM_SHEET, state.appSettings.formatSelectorStyle)
    }
}
