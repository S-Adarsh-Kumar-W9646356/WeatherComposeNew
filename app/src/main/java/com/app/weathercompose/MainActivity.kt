package com.app.weathercompose

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
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
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
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
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    val isGpsEnabled = remember { mutableStateOf(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) }

    DisposableEffect(context) {
        val gpsStatusReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                isGpsEnabled.value = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            }
        }
        val intentFilter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        context.registerReceiver(gpsStatusReceiver, intentFilter)

        onDispose {
            context.unregisterReceiver(gpsStatusReceiver)
        }
    }

    LaunchedEffect(locationPermissionState.status.isGranted, isGpsEnabled.value) {
        if (locationPermissionState.status.isGranted) {
            if (!isGpsEnabled.value) {
                showEnableGPSDialog(context)
            } else {
                getLocation(fusedLocationClient, context) { location ->
                    mainViewModel.updateLocation(location)
                }
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
        if (location != null) {
            getCityName(context, location) { cityName ->
                onLocationReceived(cityName)
            }
        } else {
            // Actively request location updates if lastLocation is null
            requestCurrentLocation(fusedLocationClient, context, onLocationReceived)
        }
    }.addOnFailureListener { exception ->
        Log.e("LocationError", "Error fetching location", exception)
    }
}

@SuppressLint("MissingPermission")
fun requestCurrentLocation(
    fusedLocationClient: FusedLocationProviderClient,
    context: Context,
    onLocationReceived: (String) -> Unit
) {
    val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY, 1000L
    ).build()


    fusedLocationClient.getCurrentLocation(
        locationRequest.priority, null
    ).addOnSuccessListener { location ->
        if (location != null) {
            getCityName(context, location) { cityName ->
                onLocationReceived(cityName)
            }
        } else {
            Log.e("LocationError", "Still unable to fetch location")
        }
    }.addOnFailureListener { exception ->
        Log.e("LocationError", "Error actively requesting location", exception)
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
            val cityName = addresses?.firstOrNull()?.locality ?: "City not found"
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


fun showEnableGPSDialog(context: Context) {
    AlertDialog.Builder(context)
        .setTitle("Enable GPS")
        .setMessage("GPS is required for getting location. Please enable GPS in settings.")
        .setPositiveButton("Settings") { _, _ ->
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            context.startActivity(intent)
        }
        .setNegativeButton("Cancel", null)
        .show()
}