package com.localdownloader.data

import com.localdownloader.domain.models.DownloadOptions
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadOptionsCodecTest {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val codec = DownloadOptionsCodec(json)

    private val sample = DownloadOptions(
        url = "https://example.com/video.mp4",
        formatId = "best",
        extractorArgs = "youtube:player_client=default,mweb",
        concurrentFragments = 2,
    )

    @Test
    fun persistThenHydrate_roundTripsOptions() {
        val hydrated = codec.hydrate(codec.persist(sample))

        assertEquals(sample, hydrated)
    }

    @Test
    fun hydrate_readsBareLegacyOptionsJson() {
        val legacy = json.encodeToString(DownloadOptions.serializer(), sample)

        assertEquals(sample, codec.hydrate(legacy))
    }

    @Test
    fun hydrate_ignoresRemovedAuthFieldsInOldRows() {
        val old = """{"options":{"url":"https://example.com/a","formatId":"18","youtubePoToken":"secret","youtubeCookiesPath":"/tmp/c.txt"},"usesSidecarSecrets":true}"""

        val hydrated = codec.hydrate(old)

        assertNotNull(hydrated)
        assertEquals("https://example.com/a", hydrated!!.url)
        assertFalse(codec.persist(hydrated).contains("secret"))
        assertFalse(codec.persist(hydrated).contains("youtubePoToken"))
    }

    @Test
    fun hydrate_returnsNullForGarbage() {
        assertNull(codec.hydrate("not json"))
        assertNull(codec.normalize("{}"))
    }
}
