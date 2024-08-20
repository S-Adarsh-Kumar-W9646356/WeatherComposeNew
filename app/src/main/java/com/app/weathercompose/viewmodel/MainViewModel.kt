package com.app.weathercompose.viewmodel

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.weathercompose.model.Weather
import com.app.weathercompose.util.Resource
import com.app.weathercompose.repo.WeatherRepository
import com.app.weathercompose.util.SearchWidgetState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: WeatherRepository
) : ViewModel() {
    private val _searchWidgetState: MutableState<SearchWidgetState> =
        mutableStateOf(value = SearchWidgetState.CLOSED)
    val searchWidgetState: State<SearchWidgetState> = _searchWidgetState

    private val _searchTextState: MutableState<String> = mutableStateOf(value = "")
    val searchTextState: State<String> = _searchTextState

    fun updateSearchWidgetState(newValue: SearchWidgetState) {
        _searchWidgetState.value = newValue
    }

    fun updateSearchTextState(newValue: String) {
        _searchTextState.value = newValue
    }

    private val _location = MutableLiveData<String?>()
    val location: LiveData<String?> = _location


    val weatherData= MutableLiveData<Resource<Weather>>(Resource.Loading())

    private var locationJob: Job? = null

    fun updateLocation(newLocation: String?) {
        _location.value = newLocation
        if (newLocation != null) {
            locationJob?.cancel()
            locationJob = viewModelScope.launch {
                delay(500) // Debounce API calls
                getWeatherData(newLocation)
            }
        }
    }

    private fun getWeatherData(location: String) {
        viewModelScope.launch {
            weatherData.value = Resource.Loading()
            try {
                weatherData.value = repository.getWeatherData(location)
            } catch (e: Exception) {
                weatherData.value = Resource.Error("Failed to fetch weather data")
            }
        }
    }
}
