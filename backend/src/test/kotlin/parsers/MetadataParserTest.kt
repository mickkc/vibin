package parsers

import kotlinx.coroutines.runBlocking
import utils.initTestDb
import de.mickkc.vibin.parsing.Parser
import de.mickkc.vibin.utils.ChecksumUtil
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MetadataParserTest {

    @Test
    fun testMp3MetadataParsing() {

        initTestDb()

        val resource = this::class.java.getResource("/test.mp3")
        assertNotNull(resource)

        val file = resource.toURI().let { java.io.File(it) }
        assertNotNull(file)

        assertTrue(file.exists())

        val parsed = runBlocking {
            Parser.parse(file)
        }

        assertNotNull(parsed)

        val ti = parsed.trackInfo

        assertEquals("Test Title", ti.title)
        repeat(6) { i ->
            assertContains(ti.artists ?: emptyList(), "Artist ${i + 1}")
        }

        assertEquals("Test Album", ti.album)

        assertEquals(4, ti.trackNumber)
        assertEquals(5, ti.trackCount)

        assertEquals(2, ti.discNumber)
        assertEquals(3, ti.discCount)

        assertNotNull(ti.tags)
        repeat(3) { i ->
            assertContains(ti.tags, "Tag ${i + 1}")
        }
        assertContains(ti.tags, "English")
        assertContains(ti.tags, "Happy")

        assertEquals(2025, ti.year)
        assertEquals("Test comment", ti.comment)

        assertEquals("[00:00.00] This is a\n[00:05.15] Test.", ti.lyrics)

        assertNotNull(ti.coverImageUrl)
        val cover = runBlocking {
            Parser.downloadCoverImage(ti.coverImageUrl)
        }
        assertNotNull(cover)
        assertEquals("5825a8465cdac38d583c72636d16f6d2", ChecksumUtil.getChecksum(cover))


        val fi = parsed.fileInfo
        assertNotNull(fi)

        assertEquals("24000", fi.sampleRate)
        assertEquals("48", fi.bitrate)
        assertEquals(1992, fi.durationMs)
        assertEquals(1, fi.getChannelsInt())
        assertEquals(file.absolutePath, fi.audioFile.file.absolutePath)
    }

}