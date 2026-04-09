package com.example.mocklyapp

import com.example.mocklyapp.data.auth.local.AuthLocalDataSource
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private const val BASE_URL = "http://10.0.2.2:8080/api/"

    private fun baseLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

    private fun createClient(authLocal: AuthLocalDataSource?): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(baseLoggingInterceptor())

        if (authLocal != null) {
            val authInterceptor = Interceptor { chain ->
                val original = chain.request()

                // не добавляем токен для /auth/*
                if (original.url.encodedPath.contains("/auth/")) {
                    return@Interceptor chain.proceed(original)
                }

                val tokens = authLocal.getTokens()
                val newRequest = if (tokens != null) {
                    original.newBuilder()
                        .addHeader("Authorization", "Bearer ${tokens.accessToken}")
                        .build()
                } else {
                    original
                }

                chain.proceed(newRequest)
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
