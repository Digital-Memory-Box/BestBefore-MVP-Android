package com.dmb.bestbefore.data.ai

import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test

class AiRepositoryTest {

    private val apiServiceApi = mockk<AiServiceApi>(relaxed = true)
    private val aiRepository = AiRepository(api = apiServiceApi)

    @Test
    fun `parseTagsJson with null element returns empty list`() {
        val result = aiRepository.parseTagsJson(null)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parseTagsJson with JsonNull returns empty list`() {
        val result = aiRepository.parseTagsJson(JsonNull.INSTANCE)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parseTagsJson with json array of string primitives extracts all tags`() {
        val jsonArray = JsonParser().parse("""["travel", "memory", "party"]""")
        val result = aiRepository.parseTagsJson(jsonArray)

        assertEquals(listOf("travel", "memory", "party"), result)
    }

    @Test
    fun `parseTagsJson with json array of objects extracts name and tag fields`() {
        val jsonArray = JsonParser().parse("""[{"name": "summer"}, {"tag": "beach"}, {"invalid": "ignore"}]""")
        val result = aiRepository.parseTagsJson(jsonArray)

        assertEquals(listOf("summer", "beach"), result)
    }

    @Test
    fun `parseTagsJson with json object containing tags array extracts tags correctly`() {
        val jsonObject = JsonParser().parse("""
            {
                "tags": [
                    "music",
                    {"name": "festival"},
                    {"tag": "concert"}
                ]
            }
        """.trimIndent())

        val result = aiRepository.parseTagsJson(jsonObject)
        assertEquals(listOf("music", "festival", "concert"), result)
    }

    @Test
    fun `parseTagsJson with category dictionary object extracts nested tag arrays`() {
        val jsonObject = JsonParser().parse("""
            {
                "mood": ["chill", "lofi"],
                "activity": ["study", "focus"]
            }
        """.trimIndent())

        val result = aiRepository.parseTagsJson(jsonObject)
        assertEquals(listOf("chill", "lofi", "study", "focus"), result)
    }

    @Test
    fun `parseTagsJson with malformed or unexpected structures fails gracefully without throwing`() {
        val invalidObject = JsonObject().apply {
            add("tags", JsonPrimitive("not-an-array"))
            add("category", JsonPrimitive(42))
        }

        val result = aiRepository.parseTagsJson(invalidObject)
        assertNotNull(result)
        assertTrue(result.isEmpty())
    }
}
