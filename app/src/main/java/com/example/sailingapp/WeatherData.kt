package com.example.sailingapp


data class WeatherResponse(
    val current_units: CurrentUnits,
    val current: CurrentWeather,
    val hourly_units: HourlyUnits,
    val hourly: HourlyWeather,
    val daily_units: DailyUnits,
    val daily: DailyWeather
)

data class CurrentUnits(
    val time: String,
    val interval: String,
    val temperature_2m: String,
    val precipitation: String,
    val wind_speed_10m: String
)

data class CurrentWeather(
    val time: String,
    val interval: Int,
    val temperature_2m: Double,
    val precipitation: Double,
    val wind_speed_10m: Double
)

data class HourlyUnits(
    val time: String,
    val temperature_2m: String,
    val precipitation_probability: String,
    val rain: String,
    val visibility: String,
    val wind_speed_10m: String
)

data class HourlyWeather(
    val time: List<String>,
    val temperature_2m: List<Double>,
    val precipitation_probability: List<Int>,
    val rain: List<Double>,
    val visibility: List<Double>,
    val wind_speed_10m: List<Double>
)

data class DailyUnits(
    val time: String,
    val temperature_2m_max: String,
    val temperature_2m_min: String,
    val sunrise: String
)

data class DailyWeather(
    val time: List<String>,
    val temperature_2m_max: List<Double>,
    val temperature_2m_min: List<Double>,
    val sunrise: List<String>
)