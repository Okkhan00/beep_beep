package com.localdownloader.downloader

object YoutubeRequestPlanner {
    data class Attempt(
        val label: String,
        val selector: String,
        val extractorArgs: String?,
    )

    fun analyzeCandidates(
        preferredExtractorArgs: String? = null,
    ): List<String?> {
        val candidates = buildList {
            preferredExtractorArgs?.trim()?.ifBlank { null }?.let(::add)
            add(null)
            // Keep analyze closer to the faster apps: try the preferred/default plan first,
            // then only a couple of web-focused fallbacks that usually unlock adaptive formats.
            add(buildExtractorArgs(clientSpec = "default,mweb"))
            add(buildExtractorArgs(clientSpec = "default,web"))
        }
        return candidates.distinct()
    }

    fun recoveryCandidates(selectedExtractorArgs: String?): List<String?> {
        val normalizedSelectedArgs = normalizeExtractorArgs(selectedExtractorArgs)
        return analyzeCandidates()
            .map(::normalizeExtractorArgs)
            .distinct()
            .filter { it != normalizedSelectedArgs }
    }

    fun buildAdaptiveSelector(
        maxHeight: Int?,
        container: String?,
        videoOnly: Boolean,
        preferredAudioLanguage: String? = null,
    ): String {
        val heightFilter = maxHeight?.let { "[height<=$it]" }.orEmpty()
        val normalizedContainer = container?.trim()?.lowercase()
        val videoExtFilter = when (normalizedContainer) {
            "mp4", "mov" -> "[ext=mp4]"
            "webm" -> "[ext=webm]"
            else -> ""
        }
        val audioExtFilter = when (normalizedContainer) {
            "mp4", "mov" -> "[ext=m4a]"
            "webm" -> "[ext=webm]"
            else -> ""
        }
        val audioLanguageFilter = languagePrefixFilter(preferredAudioLanguage)

        return if (videoOnly) {
            "bestvideo$heightFilter$videoExtFilter/bestvideo$heightFilter/bestvideo/best$heightFilter/best"
        } else {
            buildList {
                if (audioLanguageFilter != null) {
                    add("bestvideo$heightFilter$videoExtFilter+bestaudio$audioExtFilter$audioLanguageFilter")
                    add("bestvideo$heightFilter+bestaudio$audioLanguageFilter")
                }
                add("bestvideo$heightFilter$videoExtFilter+bestaudio$audioExtFilter")
                add("bestvideo$heightFilter+bestaudio")
                add("best$heightFilter")
                add("best")
            }.joinToString("/")
        }
    }

    private fun buildExtractorArgs(
        clientSpec: String,
        includePlayerSkip: Boolean = false,
    ): String {
        val parts = mutableListOf("player_client=$clientSpec")
        if (includePlayerSkip) {
            parts += "player_skip=webpage,configs"
        }
        return "youtube:${parts.joinToString(";")}"
    }

    private fun languagePrefixFilter(language: String?): String? {
        val safe = language
            ?.trim()
            ?.lowercase()
            ?.takeUnless { it.isBlank() || it == "und" }
            ?.filter { it.isLetterOrDigit() || it == '-' || it == '_' }
            ?.takeIf { it.isNotBlank() }
            ?: return null
        return "[language^=$safe]"
    }

    private fun normalizeExtractorArgs(extractorArgs: String?): String? {
        return extractorArgs?.trim()?.ifBlank { null }
    }
}
