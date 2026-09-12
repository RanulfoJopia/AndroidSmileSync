package com.example.smilesync.api

import android.content.Context
import com.example.smilesync.utils.TokenManager
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private var retrofit: Retrofit? = null
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun getClient(): OkHttpClient {
        val ctx = requireNotNull(appContext) { "RetrofitClient.init(context) must be called first" }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .connectTimeout(ApiConfig.TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(ApiConfig.TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(ApiConfig.TIMEOUT_SECONDS, TimeUnit.SECONDS)
            // Attach Bearer token from SharedPreferences on every request
            .addInterceptor { chain ->
                val token = TokenManager.getInstance(ctx).getToken()
                val request = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    .apply {
                        if (!token.isNullOrBlank()) {
                            addHeader("Authorization", "Bearer $token")
                        }
                    }
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .build()
    }

    private fun getInstance(): Retrofit {
        if (retrofit == null) {
            val gson = GsonBuilder()
                .setLenient()
                .serializeNulls()
                .create()

            retrofit = Retrofit.Builder()
                .baseUrl(ApiConfig.BASE_URL)
                .client(getClient())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        }
        return retrofit!!
    }

    val api: ApiService by lazy { getInstance().create(ApiService::class.java) }

    /** Call this after login/logout so the next request picks up the new token */
    fun resetClient() {
        retrofit = null
    }
}
