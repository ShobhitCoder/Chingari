package com.chingari.openweathermap.data.weatherdata

import com.chingari.openweathermap.data.WeatherDataSource
import com.chingari.openweathermap.data.dao.WeatherDao
import com.chingari.openweathermap.remote.RemoteSource
import com.chingari.openweathermap.remote.WeatherRemoteDataSource
import com.chingari.openweathermap.remote.WeatherResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val dao: WeatherDao,
    private val remoteDataSource: WeatherRemoteDataSource
) {

    suspend fun observeWeatherData(
        connectivityAvailable: Boolean,
        latitude: String,
        longitude: String
    ): RemoteSource<WeatherResponse> {

        return if (connectivityAvailable)
            observeRemoteData(latitude, longitude)
        else observeLocalData()

    }

    private suspend fun observeRemoteData(
        latitude: String,
        longitude: String,
    ): RemoteSource<WeatherResponse> {
        return WeatherDataSource(remoteDataSource, dao).fetchData(
            latitude,
            longitude
        )

    }

    private suspend fun observeLocalData(): RemoteSource<WeatherResponse> {
        val result = dao.getWeather();
        return RemoteSource.Success(result)

    }
}