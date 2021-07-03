package com.chingari.openweathermap.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.observe
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.chingari.openweathermap.R
import com.chingari.openweathermap.common.*
import com.chingari.openweathermap.common.GpsUtils.onGpsListener
import com.chingari.openweathermap.databinding.ActivityMainBinding
import com.chingari.openweathermap.remote.RemoteSource
import com.chingari.openweathermap.remote.WeatherResponse
import com.chingari.openweathermap.ui.weather.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks, GetLocationHandler {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val viewModel: MainViewModel by viewModels()
    private var isConnected: Boolean = true
    private var locationRequest: LocationRequest? = null
    private var locationCallback: LocationCallback? = null
    @Inject
    lateinit var preference: SharedPreferencesRepository
    private var isGPS = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.handler = this
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        GpsUtils(this).turnGPSOn(object : onGpsListener {
            override fun gpsStatus(isGPSEnable: Boolean) {
                // turn on GPS
                isGPS = isGPSEnable
            }
        })
        checkInternet()
        requestPermission()
        dataForLastLocation(preference.getLatitude(), preference.getLongitude())
        observerData()
    }

    private fun checkInternet() {
        isConnected = ConnectivityUtil.isConnected(this)
        if (!isConnected)
            Toast.makeText(
                this,
                getString(R.string.internet),
                Toast.LENGTH_SHORT
            ).show()
    }

    private fun observerData() {
        viewModel.weatherResponse.observe(this, {
            when (it) {
                is RemoteSource.Failure -> {
                    binding.progressbar.hide()
                    binding.button.isEnabled = true
                }
                is RemoteSource.Success -> {
                    binding.progressbar.hide()
                    setData(it.value)
                    binding.button.isEnabled = true
                }
                RemoteSource.Loading -> {
                    binding.progressbar.show()
                }
            }
        })
    }

    private fun requestPermission() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            return
        }
        requestForPermissionAgain()
    }


    private fun requestForPermissionAgain() {
        EasyPermissions.requestPermissions(
            this,
            getString(R.string.mandatory_permission),
            REQUEST_CODE_LOCATION_PERMISSION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {}

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        } else {
            requestPermission()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1000 -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.size > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                        return
                    }
                    fusedLocationClient.getLastLocation().addOnSuccessListener(this@MainActivity, { location ->
                            if (location != null) {

                            } else {
                                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
                            }
                        })
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun saveToSharedPreference(latitude: String, longitude: String) {
        preference.setLatitude(KEY_LATITUDE, latitude)
        preference.setLongitude(KEY_LONGITUDE, longitude)
    }

    private fun subscribeUI(latitude: String, longitude: String) {
        viewModel.observeWeatherData(
            isConnected,
            latitude,
            longitude
        )

    }


    private fun setData(value: WeatherResponse) {
        binding.location.text = value.name
        binding.temp.text = resources.getString(
            R.string.temperature_unit, value.main?.temp?.getTemperatureInString()
        )
        binding.humidity.text = resources.getString(
            R.string.humidity_value,
            value.main?.humidity.toString()
        )
        binding.pressure.text = resources.getString(
            R.string.pressure_value,
            value.main?.pressure.toString()
        )
        binding.wind.text = resources.getString(
            R.string.wind_value,
            value.wind?.speed.toString()
        )
        binding.sunrise.text =
            resources.getString(
                R.string.sunrise_value,
                value.sys?.sunrise?.getTime()
            )
        binding.sunset.text =
            resources.getString(
                R.string.sunset_value,
                value.sys?.sunset?.getTime()
            )
    }

    override fun getLocationData() {
        if (!isGPS) {
            Toast.makeText(this, "Please turn on GPS", Toast.LENGTH_SHORT).show()
            return
        }
        getLocation()
    }

    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) !== PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) !== PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    1000)
        } else {
                fusedLocationClient.getLastLocation().addOnSuccessListener(this@MainActivity, { location ->
                    if (location != null) {
                        subscribeUI(location.latitude.toString(), location.longitude.toString())
                        saveToSharedPreference(
                                location.latitude.toString(),
                                location.longitude.toString()
                        )
                    } else {
                        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
                    }
                })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 1001) {
                isGPS = true // flag maintain before get location
            }
        }
    }

    fun dataForLastLocation(latitude: String?, longitude: String?) {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {

            if (latitude != null && longitude != null) {
                subscribeUI(latitude, longitude)
            }

            return
        }
        requestForPermissionAgain()

    }


}


