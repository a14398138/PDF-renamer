package com.example.data.remote

import com.example.data.model.CrossRefResponse
import com.example.data.model.CrossRefWork
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object CrossRefClient {
    private const val BASE_URL = "https://api.crossref.org/"

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val apiService: CrossRefApiService by lazy {
        retrofit.create(CrossRefApiService::class.java)
    }

    suspend fun fetchWorkByDoi(rawDoi: String): Result<CrossRefWork> {
        return try {
            val cleanDoi = cleanDoi(rawDoi)
            if (cleanDoi.isBlank()) {
                return Result.failure(IllegalArgumentException("有効なDOIが見つかりませんでした"))
            }

            // If it's an arXiv ID without DOI prefix, format as arXiv DOI
            val targetDoi = if (cleanDoi.matches(Regex("^\\d{4}\\.\\d{4,5}(v\\d+)?$"))) {
                "10.48550/arXiv.$cleanDoi"
            } else {
                cleanDoi
            }

            val response = apiService.getWorkByDoi(targetDoi)
            if (response.isSuccessful) {
                val work = response.body()?.message
                if (work != null) {
                    Result.success(work)
                } else {
                    Result.failure(Exception("メタデータが空でした (Status: ${response.code()})"))
                }
            } else {
                Result.failure(Exception("CrossRef検索エラー: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun cleanDoi(input: String): String {
        var doi = input.trim()
        // Remove URL prefixes
        doi = doi.replace(Regex("^https?://(dx\\.)?doi\\.org/", RegexOption.IGNORE_CASE), "")
        doi = doi.replace(Regex("^doi:\\s*", RegexOption.IGNORE_CASE), "")
        // Trim trailing punctuation often caught in regex from documents
        doi = doi.trimEnd('.', ',', ';', ')', ']', '}', '>', '\"', '\'')
        return doi.trim()
    }
}
