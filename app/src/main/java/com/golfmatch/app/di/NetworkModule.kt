package com.golfmatch.app.di

import com.golfmatch.app.data.api.ApiService
import com.golfmatch.app.data.auth.AuthSessionManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/**
 * ネットワーク・Firebase関連の依存を提供するモジュール（技術設計書 2章・6章、ADR-0003）。
 *
 * バックエンドはFirebase（Firestore + Cloud Functions）を用いる。
 * [ApiService] はCloud FunctionsのHTTPSエンドポイントをRetrofit経由で呼び出すためのもので、
 * [FirebaseAuth] / [FirebaseFirestore] / [FirebaseFunctions] は今後クライアント側で
 * Firebase SDKを直接利用する場合（例: Firebase Auth電話番号認証との統合）に備えて提供する。
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Cloud FunctionsのベースURL（`functions/src/index.ts`の単一HTTPS関数`api`、リージョンは
     * `asia-northeast1`。技術設計書12-1章）。
     */
    private const val BASE_URL = "https://asia-northeast1-seiya-app-818a4.cloudfunctions.net/api/"

    @Provides
    @Singleton
    fun provideAuthInterceptor(sessionManager: AuthSessionManager): Interceptor =
        Interceptor { chain ->
            val original = chain.request()
            val token = sessionManager.accessToken
            val request = if (token != null) {
                original.newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                original
            }
            chain.proceed(request)
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: Interceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
            )
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService = retrofit.create(ApiService::class.java)

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = Firebase.firestore

    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions = Firebase.functions
}
