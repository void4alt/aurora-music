package com.auramusic.di

import com.auramusic.BuildConfig
import com.auramusic.database.AuroraDatabase
import com.auramusic.database.FavoriteDao
import com.auramusic.database.HistoryDao
import com.auramusic.database.PlaylistDao
import com.auramusic.database.SongDao
import com.auramusic.network.YouTubeApiService
import com.auramusic.repository.LibraryRepository
import com.auramusic.repository.MusicRepository
import com.auramusic.data.repository.LibraryRepositoryImpl
import com.auramusic.data.repository.MusicRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton
import android.content.Context

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AuroraDatabase = AuroraDatabase.build(context)

    @Provides
    fun provideSongDao(database: AuroraDatabase): SongDao = database.songDao()

    @Provides
    fun providePlaylistDao(database: AuroraDatabase): PlaylistDao = database.playlistDao()

    @Provides
    fun provideFavoriteDao(database: AuroraDatabase): FavoriteDao = database.favoriteDao()

    @Provides
    fun provideHistoryDao(database: AuroraDatabase): HistoryDao = database.historyDao()
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder().addInterceptor(logging).build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/youtube/v3/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideYouTubeApi(retrofit: Retrofit): YouTubeApiService =
        retrofit.create(YouTubeApiService::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindMusicRepository(impl: MusicRepositoryImpl): MusicRepository

    @Binds
    abstract fun bindLibraryRepository(impl: LibraryRepositoryImpl): LibraryRepository
}
