package com.example.data.remote

import com.example.data.model.CrossRefResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

interface CrossRefApiService {
    @Headers("User-Agent: PaperRenamer/1.0 (mailto:applet-research@aistudio.google.com)")
    @GET("works/{doi}")
    suspend fun getWorkByDoi(
        @Path(value = "doi", encoded = true) doi: String
    ): Response<CrossRefResponse>
}
