package com.dmb.bestbefore.data.ai

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit interface for the BestBefore AI Service
 * deployed at https://bestbefore-ai.up.railway.app/
 *
 * Endpoints mirror main.py routes:
 *   POST /v1/score               — Hybrid scoring (tags + dwell + location + time)
 *   POST /v1/suggestions         — Personalized room suggestions
 *   POST /v1/user-preference/update — Update preference model (tag weights + embedding)
 *   POST /v1/semantic-search     — Semantic search over room embeddings
 *   POST /v1/embed               — Generate text embedding
 *   POST /v1/generate-description — AI room description
 */
interface AiServiceApi {

    @GET("health")
    suspend fun health(): Response<Map<String, String>>

    /** POST /v1/score — returns hybrid score for two rooms */
    @POST("v1/score")
    suspend fun scoreRooms(@Body request: HybridScoreRequest): Response<HybridScoreResponse>

    /** POST /v1/suggestions — personalized room suggestions from user profile + candidates */
    @POST("v1/suggestions")
    suspend fun getSuggestions(@Body request: GenerateSuggestionsRequest): Response<GenerateSuggestionsResponse>

    /** POST /v1/user-preference/update — update preference embedding + tag weights */
    @POST("v1/user-preference/update")
    suspend fun updateUserPreference(@Body request: UpdatePreferenceRequest): Response<UpdatePreferenceResponse>

    /** POST /v1/semantic-search — find most semantically similar rooms to a query */
    @POST("v1/semantic-search")
    suspend fun semanticSearch(@Body request: SemanticSearchRequest): Response<SemanticSearchResponse>

    /** POST /v1/embed — generate a text embedding vector */
    @POST("v1/embed")
    suspend fun embed(@Body request: EmbeddingRequest): Response<EmbeddingResponse>

    /** POST /v1/generate-description — generate an AI room description */
    @POST("v1/generate-description")
    suspend fun generateDescription(@Body request: GenerateDescriptionRequest): Response<GenerateDescriptionResponse>
}
