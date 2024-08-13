package com.app.weathercompose.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app.weathercompose.model.Weather

@Dao
interface WeatherDao {
    @Query("SELECT * FROM weather WHERE LOWER(name) = LOWER(:city) LIMIT 1")
    suspend fun getWeatherByCity(city: String): Weather?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeather(weather: Weather)
}