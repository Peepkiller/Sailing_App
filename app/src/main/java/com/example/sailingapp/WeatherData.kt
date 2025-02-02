package com.example.sailingapp


data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    val hourly: Hourly,
    val hourly_units: HourlyUnits,
    val daily: Daily,
    val daily_units: DailyUnits
)

data class Hourly(
    val time: List<String>,
    val temperature_2m: List<Double>,
    val precipitation_probability: List<Int>,
    val rain: List<Double>,
    val wind_speed_10m: List<Double>
)

data class HourlyUnits(
    val temperature_2m: String,
    val precipitation_probability: String,
    val rain: String,
    val wind_speed_10m: String
)

data class Daily(
    val time: List<String>,
    val temperature_2m_max: List<Double>,
    val temperature_2m_min: List<Double>,
    val sunrise: List<String>,
    val sunset: List<String>
)

data class DailyUnits(
    val temperature_2m_max: String,
    val temperature_2m_min: String,
    val sunrise: String,
    val sunset: String
)