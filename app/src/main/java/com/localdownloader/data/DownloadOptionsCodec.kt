package com.localdownloader.data

import com.localdownloader.domain.models.DownloadOptions
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Serializes [DownloadOptions] for the persisted task store.
 *
 * Options no longer carry any credentials, cookies or tokens, so nothing is written
 * to side files. Older persisted rows (wrapper format or bare options JSON, possibly
 * containing since-removed fields) are still decoded; unknown keys are ignored by [Json].
 */
@Singleton
class DownloadOptionsCodec @Inject constructor(
    private val json: Json,
) {
    fun persist(options: DownloadOptions): String {
        return json.encodeToString(PersistedDownloadTaskOptions(options = options))
    }

    fun hydrate(optionsJson: String): DownloadOptions? {
        runCatching { json.decodeFromString<PersistedDownloadTaskOptions>(optionsJson) }
            .getOrNull()
            ?.let { return it.options }
        return runCatching { json.decodeFromString<DownloadOptions>(optionsJson) }.getOrNull()
    }

    /** Returns re-encoded JSON when [optionsJson] is decodable, or null when it is not. */
    fun normalize(optionsJson: String): String? {
        val options = hydrate(optionsJson) ?: return null
        return persist(options)
    }
}

@Serializable
internal data class PersistedDownloadTaskOptions(
    val options: DownloadOptions,
)
