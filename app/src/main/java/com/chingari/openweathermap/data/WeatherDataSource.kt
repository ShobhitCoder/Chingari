package com.chingari.openweathermap.data

import com.chingari.openweathermap.common.apiKey
import com.chingari.openweathermap.data.dao.WeatherDao
import com.chingari.openweathermap.remote.RemoteSource
import com.chingari.openweathermap.remote.WeatherRemoteDataSource
import com.chingari.openweathermap.remote.WeatherResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WeatherDataSource @Inject constructor(
    private val dataSource: WeatherRemoteDataSource,
    private val dao: WeatherDao
) {

    suspend fun fetchData(latitude: String, longitude: String): RemoteSource<WeatherResponse> {
        return withContext(Dispatchers.IO) {
            val response = dataSource
                .fetchWeatherData(apiKey = apiKey, latitude = latitude, longitude = longitude)
            when (response) {
                is RemoteSource.Success -> {
                    val result = response.value
                    dao.insert(result)
                }
            }

            response
        }

    }

}