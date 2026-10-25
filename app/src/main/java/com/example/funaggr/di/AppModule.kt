package com.example.funaggr.di

import android.content.Context
import androidx.room.Room
import com.example.funaggr.BuildConfig
import com.example.funaggr.data.geo.CachingPlaceGeocoder
import com.example.funaggr.data.geo.PlaceGeocoder
import com.example.funaggr.data.local.CacheMetadata
import com.example.funaggr.data.local.EventDao
import com.example.funaggr.data.local.EventDatabase
import com.example.funaggr.data.local.PlaceDao
import com.example.funaggr.data.local.PrefsCacheMetadata
import com.example.funaggr.data.remote.EventSource
import com.example.funaggr.data.remote.GoOutSource
import com.example.funaggr.data.remote.KudyZNudySource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android) Funaggr/${BuildConfig.VERSION_NAME} (+https://github.com/RaviSileb/Funagg)"

    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient =
        OkHttpClient.Builder()
            .cache(Cache(File(context.cacheDir, "http"), 10L * 1024 * 1024))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", USER_AGENT)
                        .header("Accept-Language", "cs-CZ,cs;q=0.9,en;q=0.5")
                        .build()
                )
            }
            .build()

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): EventDatabase =
        Room.databaseBuilder(context, EventDatabase::class.java, EventDatabase.NAME)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideEventDao(database: EventDatabase): EventDao = database.eventDao()

    @Provides
    fun providePlaceDao(database: EventDatabase): PlaceDao = database.placeDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {

    @Binds
    @IntoSet
    abstract fun bindKudyZNudy(source: KudyZNudySource): EventSource

    @Binds
    @IntoSet
    abstract fun bindGoOut(source: GoOutSource): EventSource

    @Binds
    abstract fun bindGeocoder(geocoder: CachingPlaceGeocoder): PlaceGeocoder

    @Binds
    abstract fun bindCacheMetadata(metadata: PrefsCacheMetadata): CacheMetadata
}
