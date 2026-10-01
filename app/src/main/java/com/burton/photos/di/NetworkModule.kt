package com.burton.photos.di

import android.content.Context
import coil.ImageLoader
import com.burton.photos.data.api.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun okHttpClient(auth: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(auth)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.MINUTES)
            .retryOnConnectionFailure(true)
            .build()

    @Provides
    @Singleton
    fun imageLoader(
        @ApplicationContext context: Context,
        client: OkHttpClient,
    ): ImageLoader =
        ImageLoader.Builder(context)
            .okHttpClient(client)
            .crossfade(true)
            .build()
}
