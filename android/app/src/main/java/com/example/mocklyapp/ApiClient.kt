package com.example.mocklyapp

import com.example.mocklyapp.data.auth.local.AuthLocalDataSource
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "https://api.iness.app/api/"

    private fun baseLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

    private fun createClient(authLocal: AuthLocalDataSource?): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(baseLoggingInterceptor())

        if (authLocal != null) {
            val authInterceptor = Interceptor { chain ->
                val original = chain.request()

                if (original.url.encodedPath.contains("/auth/")) {
                    return@Interceptor chain.proceed(original)
                }

                val tokens = authLocal.getTokens()
                val request = if (tokens != null) {
                    original.newBuilder()
                        .addHeader("Authorization", "Bearer ${tokens.accessToken}")
                        .build()
                } else {
                    original
                }

                chain.proceed(request)
            }

            builder.addInterceptor(authInterceptor)
        }

        return builder.build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(createClient(null))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun authedRetrofit(authLocal: AuthLocalDataSource): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(createClient(authLocal))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}