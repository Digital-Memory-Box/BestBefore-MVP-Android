package com.dmb.bestbefore.data.models

/**
 * Lightweight metadata for a single memory fetched from the backend.
 * Stored parallel to the Uri list in ProfileViewModel so the UI can look up
 * ownership and memory ID without changing the rest of the media pipeline.
 *
 * @param id       MongoDB _id of the memory document (empty for locally-added items not yet synced)
 * @param authorId MongoDB _id of the user who uploaded this memory
 */
data class MemoryItem(
    val id: String,
    val authorId: String
)
