package com.chingari.openweathermap.ui.weather

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.chingari.openweathermap.common.GET_WEATHER
import com.chingari.openweathermap.common.SYNC_WEATHER_WORKER
import com.chingari.openweathermap.data.weatherdata.WeatherRepository
import com.chingari.openweathermap.data.workmanager.MyWorkManager
import com.chingari.openweathermap.remote.RemoteSource
import com.chingari.openweathermap.remote.WeatherResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: WeatherRepository,
    private val workManager: WorkManager
) : ViewModel() {

    private var _weatherResponse = MutableLiveData<RemoteSource<WeatherResponse>>()
    val weatherResponse: LiveData<RemoteSource<WeatherResponse>>
        get() = _weatherResponse

    fun observeWeatherData(
        connectivityAvailable: Boolean,
        latitude: String,
        longitude: String
    ) {

        viewModelScope.launch {
            _weatherResponse.value = RemoteSource.Loading
            _weatherResponse.value = repository.observeWeatherData(
                connectivityAvailable,
                latitude,
                longitude
            )
        }
        startPeriodicWork()

    }

    private fun startPeriodicWork() {
        val constraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val periodicWorkRequest: PeriodicWorkRequest = PeriodicWorkRequestBuilder<MyWorkManager>(
            2, TimeUnit.HOURS
        )
            .addTag(SYNC_WEATHER_WORKER)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            GET_WEATHER,
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicWorkRequest
        )
    }

}