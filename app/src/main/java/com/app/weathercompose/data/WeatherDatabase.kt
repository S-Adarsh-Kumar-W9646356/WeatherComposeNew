package com.app.weathercompose.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.app.weathercompose.model.MainConverter
import com.app.weathercompose.model.Weather
import com.app.weathercompose.model.WeatherDetailConverter
import com.app.weathercompose.model.WindConverter

@Database(entities = [Weather::class], version = 1, exportSchema = false)
@TypeConverters(MainConverter::class, WeatherDetailConverter::class, WindConverter::class)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao
}