package com.example.nexus.api

import retrofit2.http.GET
import retrofit2.http.Query

data class TranslationResponse(val responseData: TranslationData? = null)
data class TranslationData(val translatedText: String? = null)

/** Online translation contract. MyMemory supports English, isiZulu (zu), and Setswana (tn). */
interface LanguageApiService {
    @GET("get")
    suspend fun translate(
        @Query("q") text: String,
        @Query("langpair") languagePair: String
    ): TranslationResponse
}
