package com.app.weathercompose.ui.view

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.app.weathercompose.R
import com.app.weathercompose.model.Weather
import com.app.weathercompose.ui.view.components.WeatherComponent
import com.app.weathercompose.ui.view.components.WeatherSuccessState
import com.app.weathercompose.util.Resource
import com.app.weathercompose.util.SearchWidgetState
import com.app.weathercompose.viewmodel.MainViewModel

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    navController: NavController,
    viewModel: MainViewModel
) {

    val searchTextState by viewModel.searchTextState
    val searchWidgetState by viewModel.searchWidgetState
    val weatherData by viewModel.weatherData.observeAsState(Resource.Loading())

    Log.d("tag","newLocation")

    Scaffold(
        topBar = {
            val context = LocalContext.current
            WeatherTopAppBar(
                searchWidgetState = searchWidgetState,
                searchTextState = searchTextState,
                onTextChange = { viewModel.updateSearchTextState(it) },
                onCloseClicked = { viewModel.updateSearchWidgetState(SearchWidgetState.CLOSED) },
                onSearchClicked = {
                    if (searchTextState.isEmpty()) {
                        Toast.makeText(
                            context,
                            "Please enter city name",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@WeatherTopAppBar
                    }
                    navController.navigate(
                        route = "weather_screen/${searchTextState}"
                    )
                    viewModel.updateSearchWidgetState(SearchWidgetState.CLOSED)
                    viewModel.updateSearchTextState("")
                },
                onSearchTriggered = {
                    viewModel.updateSearchWidgetState(newValue = SearchWidgetState.OPENED)
                }
            )
        },
        content = {
                paddingValues ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                color = MaterialTheme.colorScheme.background
            ) {
                when (weatherData) {
                    is Resource.Success -> {
                        weatherData.data?.let { WeatherSuccessState(modifier = Modifier, weather = it) }
                    }
                    is Resource.Error -> {
                        Text(text = weatherData.message.orEmpty())
                    }
                    is Resource.Loading -> {
                        CircularProgressIndicator()
                    }
                }
            }
        }

    )

}






        @Composable
        fun SearchAppBar(
            text: String,
            onTextChange: (String) -> Unit,
            onCloseClicked: () -> Unit,
            onSearchClicked: (String) -> Unit,
        ) {
            val keyboardController = LocalSoftwareKeyboardController.current
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                color = MaterialTheme.colorScheme.primary,
            ) {
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = text,
                    onValueChange = { onTextChange(it) },
                    placeholder = {
                        Text(
                            modifier = Modifier.alpha(0.5f),
                            text = stringResource(R.string.search_hint),
                        )
                    },
                    textStyle = TextStyle(
                        fontSize = MaterialTheme.typography.titleMedium.fontSize
                    ),
                    singleLine = true,
                    leadingIcon = {
                        IconButton(
                            modifier = Modifier.alpha(0.7f),
                            onClick = {}
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.search_icon),
                            )
                        }
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (text.isNotEmpty()) {
                                    onTextChange("")
                                } else {
                                    onCloseClicked()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.close_icon),
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            onSearchClicked(text)
                            keyboardController?.hide()
                        },
                    ),
                )
            }
        }

        @Composable
        fun WeatherTopAppBar(
            searchWidgetState: SearchWidgetState,
            searchTextState: String,
            onTextChange: (String) -> Unit,
            onCloseClicked: () -> Unit,
            onSearchClicked: (String) -> Unit,
            onSearchTriggered: () -> Unit
        ) {
            when (searchWidgetState) {
                SearchWidgetState.CLOSED -> {
                    DefaultAppBar(
                        onSearchClicked = onSearchTriggered
                    )
                }

                SearchWidgetState.OPENED -> {
                    SearchAppBar(
                        text = searchTextState,
                        onTextChange = onTextChange,
                        onCloseClicked = onCloseClicked,
                        onSearchClicked = onSearchClicked
                    )
                }
            }
        }

        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        fun DefaultAppBar(onSearchClicked: () -> Unit) {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                actions = {
                    IconButton(
                        onClick = { onSearchClicked() }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(R.string.search_icon),
                        )
                    }
                }
            )
        }
