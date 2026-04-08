package com.dmb.bestbefore.utils

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*

/**
 * DynamicRecommendationEngine — GPS (Konum) desteği eklendi.
 */
object DynamicRecommendationEngine {

    private const val OPENAI_KEY = "YOUR_OPENAI_API_KEY"
    private val client = OkHttpClient()

    /**
     * Algoritma Fonksiyonu: Score = (Tags) + (DwellTime) + (Interaction) + (Location)
     */
    suspend fun calculateRelevanceAndDecide(
        roomA: TestRoomData,
        roomB: TestRoomData
    ): RecommendationResult = withContext(Dispatchers.IO) {
        
        // 1. Etiket Benzerliği (Max 30 Puan)
        val commonTags = roomA.tags.intersect(roomB.tags.toSet()).size
        val tagScore = commonTags * 10

        // 2. Kalma Süresi (Max 20 Puan)
        val dwellScore = min(roomA.dwellTimeMinutes, 20) * 1

        // 3. Etkileşim (Max 30 Puan)
        val interactionScore = (if (roomA.hasInteraction) 15 else 0) + (if (roomB.hasInteraction) 15 else 0)

        // 4. Konum Yakınlığı (Max 20 Puan)
        val locationScore = calculateLocationScore(roomA, roomB)
        
        val baseScore = tagScore + dwellScore + interactionScore + locationScore
        
        println("[RecEngine] Skor Dağılımı -> Tag: $tagScore, Dwell: $dwellScore, Interaction: $interactionScore, Location: $locationScore")
        println("[RecEngine] Toplam Ham Puan: $baseScore")

        return@withContext when {
            baseScore > 80 -> RecommendationResult(baseScore, "DIRECT_MATCH", "Puan 80+ doğrudan eşleşti.")
            baseScore in 40..80 -> {
                println("[RecEngine] Kararsız bölge. OpenAI çağrılıyor...")
                val aiDecision = askOpenAI(roomA, roomB)
                if (aiDecision.isConnected) {
                    RecommendationResult(baseScore, "AI_CONFIRMED", "GPT-4o Onayı: ${aiDecision.reason}")
                } else {
                    RecommendationResult(baseScore, "REJECTED", "AI anlamsal bağ bulamadı.")
                }
            }
            else -> RecommendationResult(baseScore, "REJECTED", "Puan düşük, alakasız.")
        }
    }

    /**
     * Haversine Formülü ile iki koordinat arası mesafeyi ölçer.
     * Eğer odalar 1km'den yakınsa 20 tam puan verir.
     */
    private fun calculateLocationScore(a: TestRoomData, b: TestRoomData): Int {
        if (a.lat == null || a.lon == null || b.lat == null || b.lon == null) return 0
        
        val r = 6371 // Dünya yarıçapı (km)
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val haversine = sin(dLat / 2).pow(2) + 
                        cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * 
                        sin(dLon / 2).pow(2)
        val distance = 2 * r * asin(sqrt(haversine))

        return when {
            distance < 1.0 -> 20  // 1km altı: Full puan
            distance < 5.0 -> 10  // 5km altı: Orta puan
            distance < 20.0 -> 5  // 20km altı: Düşük puan
            else -> 0
        }
    }

    // ... OpenAI ve diğer metodlar aynı kalıyor ...
    suspend fun generateRoomDescription(roomName: String, tags: List<String>, imageUrls: List<String> = emptyList()): String = withContext(Dispatchers.IO) {
        val userContent = JSONArray().put(JSONObject().apply {
            put("type", "text")
            put("text", "Oda: $roomName, Etiketler: ${tags.joinToString()}. Duygusal bir açıklama yaz.")
        })
        imageUrls.forEach { url -> userContent.put(JSONObject().apply { put("type", "image_url"); put("image_url", JSONObject().put("url", url)) }) }
        val json = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("messages", JSONArray().put(JSONObject().apply { put("role", "user"); put("content", userContent) }))
        }
        val request = Request.Builder().url("https://api.openai.com/v1/chat/completions").header("Authorization", "Bearer $OPENAI_KEY")
            .post(json.toString().toRequestBody("application/json".toMediaType())).build()
        return@withContext try {
            val response = client.newCall(request).execute()
            JSONObject(response.body?.string() ?: "").getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim()
        } catch (e: Exception) { "Zamanın sessiz hikayesi..." }
    }

    private fun askOpenAI(roomA: TestRoomData, roomB: TestRoomData): AIDecision {
        val prompt = "Oda 1: ${roomA.name}, Oda 2: ${roomB.name}. Anlamsal bağ? JSON: {\"connected\": true, \"reason\": \"...\"}"
        val json = JSONObject().apply {
            put("model", "gpt-4o-mini ")
            put("messages", JSONArray().put(JSONObject().apply { put("role", "user"); put("content", prompt) }))
        }
        val request = Request.Builder().url("https://api.openai.com/v1/chat/completions").header("Authorization", "Bearer $OPENAI_KEY")
            .post(json.toString().toRequestBody("application/json".toMediaType())).build()
        return try {
            val choice = JSONObject(client.newCall(request).execute().body?.string() ?: "").getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
            val clean = choice.replace("```json", "").replace("```", "").trim()
            val d = JSONObject(clean)
            AIDecision(d.getBoolean("connected"), d.getString("reason"))
        } catch (e: Exception) { AIDecision(false, "Hata") }
    }
}

data class TestRoomData(
    val name: String, 
    val tags: List<String>, 
    val dwellTimeMinutes: Int, 
    val hasInteraction: Boolean,
    val lat: Double? = null,
    val lon: Double? = null
)
data class RecommendationResult(val score: Int, val status: String, val reason: String)
data class AIDecision(val isConnected: Boolean, val reason: String)
