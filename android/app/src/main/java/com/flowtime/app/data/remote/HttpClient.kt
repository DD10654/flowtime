package com.flowtime.app.data.remote

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/** Builds the [ApiService] against a base URL (see BuildConfig.API_BASE_URL). */
object HttpClient {

    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = true
        encodeDefaults = true
        coerceInputValues = true
        isLenient = true
    }

    fun create(baseUrl: String): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(JsonConverterFactory(json, contentType))
            .build()
            .create(ApiService::class.java)
    }
}
