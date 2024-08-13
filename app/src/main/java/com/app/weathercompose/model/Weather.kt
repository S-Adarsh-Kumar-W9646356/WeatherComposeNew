package com.app.weathercompose.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.adematici.weatherapp.model.WeatherDetail
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

@Entity(tableName = "weather")
data class Weather(
    @PrimaryKey
    @SerializedName("id")
    val id: Int,
    @SerializedName("main")
    @TypeConverters(MainConverter::class)
    val main: Main,
    @SerializedName("name")
    val name: String,
    @SerializedName("weather")
    @TypeConverters(WeatherDetailConverter::class)
    val weather: List<WeatherDetail>,
    @SerializedName("wind")
    @TypeConverters(WindConverter::class)
    val wind: Wind,
    val latitude: Double=0.0,
    val longitude: Double=0.0,
)
class MainConverter {
    @TypeConverter
    fun fromMain(main: Main): String {
        return Gson().toJson(main)
    }

    @TypeConverter
    fun toMain(mainString: String): Main {
        val type = object : TypeToken<Main>() {}.type
        return Gson().fromJson(mainString, type)
    }
}
class WeatherDetailConverter {
    @TypeConverter
    fun fromWeatherDetailList(weatherDetails: List<WeatherDetail>): String {
        return Gson().toJson(weatherDetails)
    }

    @TypeConverter
    fun toWeatherDetailList(weatherDetailsString: String): List<WeatherDetail> {
        val type = object : TypeToken<List<WeatherDetail>>() {}.type
        return Gson().fromJson(weatherDetailsString, type)
    }
}
class WindConverter {
    @TypeConverter
    fun fromWind(wind: Wind): String {
        return Gson().toJson(wind)
    }

    @TypeConverter
    fun toWind(windString: String): Wind {
        val type = object : TypeToken<Wind>() {}.type
        return Gson().fromJson(windString, type)
    }
}