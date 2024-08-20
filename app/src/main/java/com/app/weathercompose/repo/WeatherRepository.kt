package com.app.weathercompose.repo

import android.util.Log
import com.app.weathercompose.model.Weather
import com.app.weathercompose.util.Resource
import com.app.weathercompose.data.WeatherDao
import com.app.weathercompose.service.WeatherAPI
import dagger.hilt.android.scopes.ActivityScoped
import java.lang.Exception
import javax.inject.Inject

@ActivityScoped
class WeatherRepository @Inject constructor(
    private val api: WeatherAPI,
    private val weatherDao: WeatherDao
) {
    suspend fun getWeatherData(city: String): Resource<Weather> {
        return try {
            Log.d("RESPONSE", "city: $city }")
            val response = api.getWeatherData(city = city)
            weatherDao.insertWeather(response) // Store the data in the local database
            Resource.Success(response)
        } catch (e: Exception) {
            // Fetch data from local database if there is no internet connection
            e.printStackTrace()
            val cachedWeather = weatherDao.getWeatherByCity(city)
            if (cachedWeather != null) {
                Resource.Success(cachedWeather)
            } else {
                Resource.Error("No internet connection and no cached data available.")
            }
        }
    }
}