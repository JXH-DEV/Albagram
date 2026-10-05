package com.albagram.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface DictionaryApiService {
    @GET("v1/dictionary/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("mode") mode: String = "contains",
        @Query("limit") limit: Int = 20
    ): DictionarySearchResponseDto
}
