package de.pvcompact.app

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.Locale

/**
 * Solar forecast with two providers:
 * 1) Forecast.Solar when the user has configured a Personal API key.
 * 2) Open-Meteo as a keyless fallback. Open-Meteo returns global tilted irradiance;
 *    the app turns this into a PV estimate using kWp and a configurable performance ratio.
 */
class PvForecastApi(private val config: ForecastConfig) {

    fun load(): ForecastData {
        if (config.forecastSolarApiKey.isNotBlank()) {
            runCatching { return loadForecastSolar() }
        }
        return loadOpenMeteo()
    }

    private fun loadForecastSolar(): ForecastData {
        val c = config
        val key = URLEncoder.encode(c.forecastSolarApiKey, StandardCharsets.UTF_8.toString())
        val url = String.format(
            Locale.US,
            "https://api.forecast.solar/%s/estimate/%.5f/%.5f/%d/%d/%.3f",
            key, c.latitude, c.longitude, c.tilt, c.azimuth, c.kwp
        )
        val root = JSONObject(get(url))
        val result = root.getJSONObject("result")
        val daily = result.getJSONObject("watt_hours_day")
        val dates = daily.keys().asSequence().toList().sorted()
        val days = dates.map { date -> ForecastDay(date, daily.optDouble(date, 0.0)) }
        return ForecastData(
            days = days,
            source = "Forecast.Solar",
            note = "Direktes PV-Prognosemodell"
        )
    }

    private fun loadOpenMeteo(): ForecastData {
        val c = config
        val url = String.format(
            Locale.US,
            "https://api.open-meteo.com/v1/forecast?latitude=%.5f&longitude=%.5f" +
                "&hourly=global_tilted_irradiance" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min" +
                "&tilt=%d&azimuth=%d&forecast_days=4&timezone=auto",
            c.latitude, c.longitude, c.tilt, c.azimuth
        )
        val root = JSONObject(get(url))
        val hourly = root.getJSONObject("hourly")
        val times = hourly.getJSONArray("time")
        val gti = hourly.getJSONArray("global_tilted_irradiance")

        val energyByDate = linkedMapOf<String, Double>()
        for (i in 0 until minOf(times.length(), gti.length())) {
            val time = times.optString(i)
            if (time.length < 10 || gti.isNull(i)) continue
            val date = time.substring(0, 10)
            val irradiance = gti.optDouble(i, 0.0).coerceAtLeast(0.0)
            // GTI is an hourly mean in W/m². At 1000 W/m², kWp corresponds to kW output.
            // Thus irradiance * kWp gives Wh for the hour; PR accounts for system losses.
            energyByDate[date] = (energyByDate[date] ?: 0.0) + irradiance * c.kwp * c.performanceRatio
        }

        val weather = root.optJSONObject("daily")
        val dailyDates = weather?.optJSONArray("time")
        val codes = weather?.optJSONArray("weather_code")
        val minTemps = weather?.optJSONArray("temperature_2m_min")
        val maxTemps = weather?.optJSONArray("temperature_2m_max")
        val weatherIndex = mutableMapOf<String, Int>()
        if (dailyDates != null) {
            for (i in 0 until dailyDates.length()) weatherIndex[dailyDates.optString(i)] = i
        }

        val today = LocalDate.now()
        val days = energyByDate.entries
            .filter { runCatching { !LocalDate.parse(it.key).isBefore(today) }.getOrDefault(false) }
            .sortedBy { it.key }
            .take(4)
            .map { (date, wh) ->
                val i = weatherIndex[date]
                ForecastDay(
                    date = date,
                    energyWh = wh,
                    weatherCode = i?.let { codes?.optInt(it) },
                    temperatureMinC = i?.let { if (minTemps?.isNull(it) == false) minTemps.optDouble(it) else null },
                    temperatureMaxC = i?.let { if (maxTemps?.isNull(it) == false) maxTemps.optDouble(it) else null }
                )
            }

        return ForecastData(
            days = days,
            source = "Open-Meteo",
            note = "Aus Wetter/Einstrahlung berechnet · Anlagenfaktor ${(c.performanceRatio * 100).toInt()} %"
        )
    }

    private fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 12000
            connection.readTimeout = 12000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "PVCompact/0.2 Android")
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = BufferedReader(InputStreamReader(stream)).use { it.readText() }.trim()
            if (code !in 200..299) throw IllegalStateException("Forecast Fehler $code: ${body.take(160)}")
            return body
        } finally {
            connection.disconnect()
        }
    }
}
