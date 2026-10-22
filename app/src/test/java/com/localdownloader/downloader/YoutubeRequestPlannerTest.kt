package com.localdownloader.downloader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeRequestPlannerTest {

    @Test
    fun recoveryCandidates_excludesSelectedExtractorArgs() {
        val selected = "youtube:player_client=default,android"

        val candidates = YoutubeRequestPlanner.recoveryCandidates(
            selectedExtractorArgs = selected,
        )

        assertFalse(candidates.contains(selected))
        assertTrue(candidates.contains(null))
        assertTrue(candidates.any { it?.contains("default,mweb") == true })
    }

    @Test
    fun analyzeCandidates_prioritizePreferredPlanBeforeFallbacks() {
        val preferred = "youtube:player_client=default,web;player_skip=webpage,configs"

        val candidates = YoutubeRequestPlanner.analyzeCandidates(
            preferredExtractorArgs = preferred,
        )

        assertEquals(preferred, candidates.first())
        assertTrue(candidates.contains(null))
        assertFalse(candidates.any { it?.contains("default,android") == true })
    }

    @Test
    fun buildAdaptiveSelector_preservesRequestedHeightAndContainer() {
        val selector = YoutubeRequestPlanner.buildAdaptiveSelector(
            maxHeight = 1080,
            container = "mp4",
            videoOnly = false,
        )

        assertEquals(
            "bestvideo[height<=1080][ext=mp4]+bestaudio[ext=m4a]/bestvideo[height<=1080]+bestaudio/best[height<=1080]/best",
            selector,
        )
    }

    @Test
    fun buildAdaptiveSelector_prefersRequestedAudioLanguage() {
        val selector = YoutubeRequestPlanner.buildAdaptiveSelector(
            maxHeight = 1080,
            container = "mp4",
            videoOnly = false,
            preferredAudioLanguage = "hi",
        )

        assertTrue(selector.startsWith("bestvideo[height<=1080][ext=mp4]+bestaudio[ext=m4a][language^=hi]"))
        assertTrue(selector.contains("bestvideo[height<=1080]+bestaudio[language^=hi]"))
    }

}
