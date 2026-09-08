package com.dmb.bestbefore.utils

import com.dmb.bestbefore.data.models.HallwayCard

/**
 * Pure reducer functions for Hallway card search and tag filtering.
 * Has zero Android framework or network dependencies.
 */
object HallwayFilterReducer {

    /**
     * Filters a list of [HallwayCard] items based on tag selection and search query.
     * When [searchQuery] is 3+ characters and [semanticSearchCards] is non-empty,
     * uses the backend semantic search results as the base list before applying tag filters.
     * Otherwise, matches [searchQuery] case-insensitively against title, description, and tags.
     */
    fun filterCards(
        cards: List<HallwayCard>,
        selectedTag: String?,
        searchQuery: String,
        semanticSearchCards: List<HallwayCard> = emptyList()
    ): List<HallwayCard> {
        val normalizedQuery = searchQuery.trim()
        val sourceCards = if (normalizedQuery.length >= 3 && semanticSearchCards.isNotEmpty()) {
            semanticSearchCards
        } else {
            cards
        }

        return sourceCards.filter { card ->
            val matchesTagFilter = selectedTag.isNullOrBlank() || card.tags.any { it.equals(selectedTag, ignoreCase = true) }
            val matchesSearch = if (normalizedQuery.isBlank() || (normalizedQuery.length >= 3 && semanticSearchCards.isNotEmpty())) {
                true
            } else {
                card.title.contains(normalizedQuery, ignoreCase = true) ||
                        card.description.contains(normalizedQuery, ignoreCase = true) ||
                        card.tags.any { it.contains(normalizedQuery, ignoreCase = true) }
            }
            matchesTagFilter && matchesSearch
        }
    }
}
