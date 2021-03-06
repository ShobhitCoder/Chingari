package com.chingari.openweathermap.di

import android.app.Application
import android.content.Context
import androidx.work.WorkManager
import com.google.gson.Gson
import com.chingari.openweathermap.BuildConfig
import com.chingari.openweathermap.common.SharedPreferencesRepository
import com.chingari.openweathermap.data.AppDatabase
import com.chingari.openweathermap.data.WeatherDataSource
import com.chingari.openweathermap.data.workmanager.MyWorkerFactory
import com.chingari.openweathermap.remote.ApiService
import com.chingari.openweathermap.remote.WeatherRemoteDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideWeatherService(
        @WeatherApi okhttpClient: OkHttpClient,
        converterFactory: GsonConverterFactory
    ) = provideService(okhttpClient, converterFactory, ApiService::class.java)

    private fun <T> provideService(
        okhttpClient: OkHttpClient,
        converterFactory: GsonConverterFactory, clazz: Class<T>
    ): T {
        return createRetrofit(okhttpClient, converterFactory).create(clazz)
    }

    @Singleton
    @Provides
    fun provideWeatherRemoteDataSource(weatherService: ApiService) =
        WeatherRemoteDataSource(weatherService)

    @WeatherApi
    @Provides
    fun providePrivateOkHttpClient(
        upstreamClient: OkHttpClient
    ): OkHttpClient {
        return upstreamClient.newBuilder().build()
    }

    @Provides
    fun provideOkHttpClient(interceptor: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient.Builder().addInterceptor(interceptor)
            .build()

    @Provides
    fun provideLoggingInterceptor() =
        HttpLoggingInterceptor().apply {
            level =
                if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

    @Provides
    @Singleton
    fun provideGsonConverterFactory(gson: Gson): GsonConverterFactory =
        GsonConverterFactory.create(gson)


    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Singleton
    @Provides
    fun provideApplcationContext(app: Application): Context = app.applicationContext


    private fun createRetrofit(
        okhttpClient: OkHttpClient,
        converterFactory: GsonConverterFactory
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ApiService.BASE_URL)
            .client(okhttpClient)
            .addConverterFactory(converterFactory)
            .build()
    }


    @Singleton
    @Provides
    fun provideWorkerFactory(
        weatherDataSource: WeatherDataSource,
        preference: SharedPreferencesRepository
    ) = MyWorkerFactory(weatherDataSource, preference)

    @Singleton
    @Provides
    fun provideWorkManager(app: Application) = WorkManager.getInstance(app.applicationContext)

    @Singleton
    @Provides
    fun provideDb(app: Application) = AppDatabase.getInstance(app)

    @Singleton
    @Provides
    fun provideNewsSetDao(db: AppDatabase) = db.getWeatherDao()

}