package com.dmb.bestbefore.utils

import com.dmb.bestbefore.data.models.HallwayCard
import org.junit.Assert.*
import org.junit.Test

class HallwayFilterReducerTest {

    private val card1 = HallwayCard(
        id = "card-1",
        title = "Summer Roadtrip",
        description = "Exploring the Pacific Coast highway",
        tags = listOf("Travel", "Summer", "Adventure")
    )

    private val card2 = HallwayCard(
        id = "card-2",
        title = "Coding Camp 2026",
        description = "Kotlin and Compose deep dive",
        tags = listOf("Tech", "Coding", "Android")
    )

    private val card3 = HallwayCard(
        id = "card-3",
        title = "Art & Design Showcase",
        description = "Exhibition of digital memory boxes",
        tags = listOf("Art", "Design", "Exhibition")
    )

    private val allCards = listOf(card1, card2, card3)

    @Test
    fun `filterCards with empty query and null tag returns all cards`() {
        val result = HallwayFilterReducer.filterCards(
            cards = allCards,
            selectedTag = null,
            searchQuery = ""
        )

        assertEquals(3, result.size)
        assertEquals(allCards, result)
    }

    @Test
    fun `filterCards with tag filter matches case-insensitively`() {
        val result = HallwayFilterReducer.filterCards(
            cards = allCards,
            selectedTag = "travel",
            searchQuery = ""
        )

        assertEquals(1, result.size)
        assertEquals("card-1", result.first().id)
    }

    @Test
    fun `filterCards with tag filter matching no cards returns empty list`() {
        val result = HallwayFilterReducer.filterCards(
            cards = allCards,
            selectedTag = "NonExistentTag",
            searchQuery = ""
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `filterCards matches search query against title, description, and tags case-insensitively`() {
        // Match against title
        val titleMatch = HallwayFilterReducer.filterCards(allCards, null, "summer")
        assertEquals(1, titleMatch.size)
        assertEquals("card-1", titleMatch.first().id)

        // Match against description
        val descMatch = HallwayFilterReducer.filterCards(allCards, null, "compose")
        assertEquals(1, descMatch.size)
        assertEquals("card-2", descMatch.first().id)

        // Match against tags
        val tagMatch = HallwayFilterReducer.filterCards(allCards, null, "exhibition")
        assertEquals(1, tagMatch.size)
        assertEquals("card-3", tagMatch.first().id)
    }

    @Test
    fun `filterCards with both tag filter and search query applies AND logic`() {
        // Tag "Tech" + query "Kotlin" -> matches card2
        val match = HallwayFilterReducer.filterCards(allCards, "Tech", "kotlin")
        assertEquals(1, match.size)
        assertEquals("card-2", match.first().id)

        // Tag "Tech" + query "Summer" -> 0 matches (tag matches card2, query matches card1)
        val noMatch = HallwayFilterReducer.filterCards(allCards, "Tech", "summer")
        assertTrue(noMatch.isEmpty())
    }

    @Test
    fun `filterCards with semantic search results uses semantic cards when query length is 3 or more`() {
        val semanticCard = HallwayCard(
            id = "semantic-1",
            title = "AI Re-ranked Result",
            description = "High relevance from embeddings",
            tags = listOf("AI", "Smart")
        )

        val result = HallwayFilterReducer.filterCards(
            cards = allCards,
            selectedTag = null,
            searchQuery = "vacation",
            semanticSearchCards = listOf(semanticCard)
        )

        assertEquals(1, result.size)
        assertEquals("semantic-1", result.first().id)
    }

    @Test
    fun `filterCards handles special characters, unicode, and malformed queries safely`() {
        val weirdQueries = listOf(
            "[.*+?^\${}()|\\/]",
            "   \n\t   ",
            "🎉🚀✨",
            "\"'<>%&",
            "null",
            "!@#$%^&*()"
        )

        for (query in weirdQueries) {
            val result = HallwayFilterReducer.filterCards(
                cards = allCards,
                selectedTag = null,
                searchQuery = query
            )
            assertNotNull(result)
        }
    }
}
