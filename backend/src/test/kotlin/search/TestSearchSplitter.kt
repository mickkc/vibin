package search

import wtf.ndu.vibin.search.SearchQueryBuilder
import kotlin.test.Test
import kotlin.test.assertContentEquals

class TestSearchSplitter {

    @Test
    fun testSimpleSplit() {
        val input = "a:beatles t:help"
        val expected = listOf("a:beatles", "t:help")
        val result = SearchQueryBuilder.split(input)
        assertContentEquals(expected, result)
    }

    @Test
    fun testQuotedSplit() {
        val input = "a:\"the beatles\" t:help"
        val expected = listOf("a:\"the beatles\"", "t:help")
        val result = SearchQueryBuilder.split(input)
        assertContentEquals(expected, result)
    }

    @Test
    fun testBracketedSplit() {
        val input = "(a:beatles OR a:\"the beatles\") t:help"
        val expected = listOf("(a:beatles OR a:\"the beatles\")", "t:help")
        val result = SearchQueryBuilder.split(input)
        assertContentEquals(expected, result)
    }
}