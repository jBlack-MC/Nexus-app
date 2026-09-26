package com.example.nexus.api

/** Optional online translation. The app can continue with local copy when the network is unavailable. */
object OnlineLanguageRepository {
    suspend fun translate(
        text: String,
        source: String = "en",
        target: String,
    ): String? =
        runCatching {
            RetrofitClient.languageInstance
                .translate(text = text, languagePair = "$source|$target")
                .responseData
                ?.translatedText
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
}
