package search

import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.assertThrows
import utils.initTestDb
import wtf.ndu.vibin.search.SearchQueryBuilder
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TestSearchBuilder {

    @BeforeTest
    fun setup() {
        initTestDb()
    }

    @Test
    fun testTitleSearch() = transaction {
        val input = "t:\"Never Gonna Give You Up\""
        val op = SearchQueryBuilder.build(input)
        assertEquals("LOWER(TRACK.TITLE) LIKE '%never gonna give you up%'", op.toString())
    }

    @Test
    fun testAlbumSearch() = transaction {
        val input = "al:\"Whenever You Need Somebody\""
        val op = SearchQueryBuilder.build(input)
        assertEquals("TRACK.ALBUM_ID IN (SELECT ALBUM.ID FROM ALBUM WHERE LOWER(ALBUM.TITLE) LIKE '%whenever you need somebody%')", op.toString())
    }

    @Test
    fun testArtistSearch() = transaction {
        val input = "a:\"Rick Astley\""
        val op = SearchQueryBuilder.build(input)
        assertEquals("TRACK.ID IN (SELECT TRACK_ARTIST.TRACK_ID FROM TRACK_ARTIST WHERE TRACK_ARTIST.ARTIST_ID IN (SELECT ARTIST.ID FROM ARTIST WHERE LOWER(ARTIST.\"name\") LIKE '%rick astley%'))", op.toString())
    }

    @Test
    fun testSingleYearSearch() = transaction {
        val input = "y:1987"
        val op = SearchQueryBuilder.build(input)
        assertEquals("TRACK.\"year\" = 1987", op.toString())
    }

    @Test
    fun testGteYearSearch() = transaction {
        val input = "y:1987-"
        val op = SearchQueryBuilder.build(input)
        assertEquals("(TRACK.\"year\" IS NOT NULL) AND (TRACK.\"year\" >= 1987)", op.toString())
    }

    @Test
    fun testLteYearSearch() = transaction {
        val input = "y:-1987"
        val op = SearchQueryBuilder.build(input)
        assertEquals("(TRACK.\"year\" IS NOT NULL) AND (TRACK.\"year\" <= 1987)", op.toString())
    }

    @Test
    fun testRangeYearSearch() = transaction {
        val input = "y:1980-1987"
        val op = SearchQueryBuilder.build(input)
        assertEquals("(TRACK.\"year\" IS NOT NULL) AND (TRACK.\"year\" >= 1980) AND (TRACK.\"year\" <= 1987)", op.toString())
    }

    @Test
    fun testExplicit() = transaction {
        val input = "e:no"
        val op = SearchQueryBuilder.build(input)
        assertEquals("TRACK.EXPLICIT = FALSE", op.toString())
    }

    @Test
    fun testTagInclude() = transaction {
        val input = "+pop"
        val op = SearchQueryBuilder.build(input)
        assertEquals("TRACK.ID IN (SELECT TRACK_TAG.TRACK_ID FROM TRACK_TAG WHERE TRACK_TAG.TAG_ID IN (SELECT TAG.ID FROM TAG WHERE LOWER(TAG.\"name\") = 'pop'))", op.toString())
    }

    @Test
    fun testTagExclude() = transaction {
        val input = "-rock"
        val op = SearchQueryBuilder.build(input)
        assertEquals("TRACK.ID NOT IN (SELECT TRACK_TAG.TRACK_ID FROM TRACK_TAG WHERE TRACK_TAG.TAG_ID IN (SELECT TAG.ID FROM TAG WHERE LOWER(TAG.\"name\") = 'rock'))", op.toString())
    }

    @Test
    fun testDefaultSearch() = transaction {
        val input = "\"Never Gonna Give You Up\""
        val op = SearchQueryBuilder.build(input)
        assertEquals("(LOWER(TRACK.TITLE) LIKE '%never gonna give you up%') " +
                "OR (TRACK.ID IN (SELECT TRACK_ARTIST.TRACK_ID FROM TRACK_ARTIST WHERE TRACK_ARTIST.ARTIST_ID IN (SELECT ARTIST.ID FROM ARTIST WHERE LOWER(ARTIST.\"name\") LIKE '%never gonna give you up%'))) " +
                "OR (TRACK.ALBUM_ID IN (SELECT ALBUM.ID FROM ALBUM WHERE LOWER(ALBUM.TITLE) LIKE '%never gonna give you up%'))", op.toString())
    }

    @Test
    fun testDurationSearch() = transaction {
        val input = "d:200000-300000"
        val op = SearchQueryBuilder.build(input)
        assertEquals("(TRACK.DURATION IS NOT NULL) AND (TRACK.DURATION >= 200000) AND (TRACK.DURATION <= 300000)", op.toString())
    }

    @Test
    fun testDurationGteSearch() = transaction {
        val input = "d:200000-"
        val op = SearchQueryBuilder.build(input)
        assertEquals("(TRACK.DURATION IS NOT NULL) AND (TRACK.DURATION >= 200000)", op.toString())
    }

    @Test
    fun testDurationLteSearch() = transaction {
        val input = "d:-300000"
        val op = SearchQueryBuilder.build(input)
        assertEquals("(TRACK.DURATION IS NOT NULL) AND (TRACK.DURATION <= 300000)", op.toString())
    }

    @Test
    fun testDurationExactSearch() = transaction {
        val input = "d:250000"
        val op = SearchQueryBuilder.build(input)
        assertEquals("TRACK.DURATION = 250000", op.toString())
    }


    @Test
    fun testBitrateSearch() = transaction {
        val input = "b:256-320"
        val op = SearchQueryBuilder.build(input)
        assertEquals("(TRACK.BITRATE IS NOT NULL) AND (TRACK.BITRATE >= 256) AND (TRACK.BITRATE <= 320)", op.toString())
    }

    @Test
    fun testBitrateGteSearch() = transaction {
        val input = "b:256-"
        val op = SearchQueryBuilder.build(input)
        assertEquals("(TRACK.BITRATE IS NOT NULL) AND (TRACK.BITRATE >= 256)", op.toString())
    }

    @Test
    fun testBitrateLteSearch() = transaction {
        val input = "b:-320"
        val op = SearchQueryBuilder.build(input)
        assertEquals("(TRACK.BITRATE IS NOT NULL) AND (TRACK.BITRATE <= 320)", op.toString())
    }

    @Test
    fun testBitrateExactSearch() = transaction {
        val input = "b:320"
        val op = SearchQueryBuilder.build(input)
        assertEquals("TRACK.BITRATE = 320", op.toString())
    }

    @Test
    fun testComplexSearch1() = transaction {
        val input = "a:\"Rick Astley\" AND al:\"Whenever You Need Somebody\" AND t:\"Never Gonna Give You Up\" AND y:1987 AND +pop AND -rock AND e:no"
        val op = SearchQueryBuilder.build(input)
        assertEquals("(TRACK.ID IN (SELECT TRACK_ARTIST.TRACK_ID FROM TRACK_ARTIST WHERE TRACK_ARTIST.ARTIST_ID IN (SELECT ARTIST.ID FROM ARTIST WHERE LOWER(ARTIST.\"name\") LIKE '%rick astley%'))) " +
                "AND (TRACK.ALBUM_ID IN (SELECT ALBUM.ID FROM ALBUM WHERE LOWER(ALBUM.TITLE) LIKE '%whenever you need somebody%')) " +
                "AND (LOWER(TRACK.TITLE) LIKE '%never gonna give you up%') " +
                "AND (TRACK.\"year\" = 1987) " +
                "AND (TRACK.ID IN (SELECT TRACK_TAG.TRACK_ID FROM TRACK_TAG WHERE TRACK_TAG.TAG_ID IN (SELECT TAG.ID FROM TAG WHERE LOWER(TAG.\"name\") = 'pop'))) " +
                "AND (TRACK.ID NOT IN (SELECT TRACK_TAG.TRACK_ID FROM TRACK_TAG WHERE TRACK_TAG.TAG_ID IN (SELECT TAG.ID FROM TAG WHERE LOWER(TAG.\"name\") = 'rock'))) " +
                "AND (TRACK.EXPLICIT = FALSE)", op.toString())
    }

    @Test
    fun testInvalidYearSearch() = transaction {
        val input = "y:abcd"
        val op = SearchQueryBuilder.build(input)
        assertEquals("TRUE", op.toString())
    }

    @Test
    fun testInvalidQuotes() = transaction {
        val input = "a:\"Rick Astley"
        assertThrows<IllegalArgumentException>("Unclosed quotes in query") {
            SearchQueryBuilder.build(input)
        }
        return@transaction
    }

    @Test
    fun testInvalidBrackets() = transaction {
        val input = "(a:beatles OR a:\"the beatles\" t:help"
        assertThrows<IllegalArgumentException>("Unclosed brackets in query") {
            SearchQueryBuilder.build(input)
        }
        return@transaction
    }

    @Test
    fun testOrSearch() = transaction {
        val input = "t:test1 OR t:test2"
        val op = SearchQueryBuilder.build(input)
        assertEquals("(LOWER(TRACK.TITLE) LIKE '%test1%') OR (LOWER(TRACK.TITLE) LIKE '%test2%')", op.toString())
    }

    @Test
    fun testAndOrSearch() = transaction {
        val input = "t:test1 AND t:test2 OR t:test3"
        val op = SearchQueryBuilder.build(input)
        assertEquals("((LOWER(TRACK.TITLE) LIKE '%test1%') AND (LOWER(TRACK.TITLE) LIKE '%test2%')) OR (LOWER(TRACK.TITLE) LIKE '%test3%')", op.toString())
    }

    @Test
    fun testEmptyInput() = transaction {
        val input = "   "
        val op = SearchQueryBuilder.build(input)
        assertEquals("TRUE", op.toString())
    }

    @Test
    fun testDefaultAnd() = transaction {
        val input = "t:test1 t:test2"
        val op = SearchQueryBuilder.build(input)
        assertEquals("(LOWER(TRACK.TITLE) LIKE '%test1%') AND (LOWER(TRACK.TITLE) LIKE '%test2%')", op.toString())
    }
}