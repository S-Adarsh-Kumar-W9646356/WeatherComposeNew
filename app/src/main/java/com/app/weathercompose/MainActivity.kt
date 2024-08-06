package com.app.weathercompose

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.app.weathercompose.ui.theme.WeatherComposeTheme
import com.app.weathercompose.ui.view.MainScreen
import com.app.weathercompose.ui.view.SplashScreen
import com.app.weathercompose.ui.view.WeatherScreen
import com.app.weathercompose.viewmodel.MainViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.security.Permission
import java.util.Locale


@ExperimentalAnimationApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WeatherComposeTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    RequestLocationPermission(mainViewModel)
                    Navigation(mainViewModel)
                }
            }
        }
    }
}

@ExperimentalAnimationApi
@Composable
fun Navigation(mainViewModel: MainViewModel) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "splash_screen"
    ) {
        composable("splash_screen") {
            SplashScreen(navController)
        }
        composable("main_screen") {
            MainScreen(navController, viewModel = mainViewModel)
        }
        composable(
            "weather_screen/{city}",
            arguments = listOf(
                navArgument("city") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            WeatherScreen(
                city = backStackEntry.arguments?.getString("city") ?: ""
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestLocationPermission(mainViewModel: MainViewModel) {
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
            getLocation(fusedLocationClient, context) { location ->
                mainViewModel.updateLocation(location)
            }
        } else {
            locationPermissionState.launchPermissionRequest()
        }
    }
}


@SuppressLint("MissingPermission")
fun getLocation(
    fusedLocationClient: FusedLocationProviderClient,
    context: Context,
    onLocationReceived: (String) -> Unit
) {
    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        location?.let {
            getCityName(context, location) { cityName ->
                onLocationReceived(cityName)
            }
        } ?: onLocationReceived("Location not found")
    }.addOnFailureListener { exception ->
        Log.e("LocationError", "Error fetching location", exception)
        onLocationReceived("Error fetching location")
    }
}

private fun getCityName(
    context: Context,
    location: Location,
    onCityNameReceived: (String) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            val cityName = if (!addresses.isNullOrEmpty()) {
                addresses[0].locality ?: "City not found"
            } else {
                "City not found"
            }
            withContext(Dispatchers.Main) {
                onCityNameReceived(cityName)
            }
        } catch (e: IOException) {
            Log.e("GeocodingError", "Error fetching city name", e)
            withContext(Dispatchers.Main) {
                onCityNameReceived("Error fetching city name")
            }
        }
    }
}