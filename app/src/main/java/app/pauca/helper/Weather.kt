package app.pauca.helper

import android.content.Context
import app.pauca.R
import app.pauca.data.Prefs
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.roundToInt

/**
 * Tempo da tela inicial, pelo Open-Meteo (sem chave e sem permissão de localização: a cidade
 * é digitada). As funções de rede bloqueiam; chame fora da thread principal.
 */
object Weather {

    /** No máximo uma busca a cada tanto, quando a tela inicial volta. */
    const val REFRESH_MINUTES = 45

    class City(val name: String, val lat: Double, val lon: Double, val region: String = "", val country: String = "") {
        /** "Cidade, Região, País" sem partes vazias nem repetidas. */
        val label: String get() = listOf(name, region, country).filter { it.isNotBlank() }.distinct().joinToString(", ")
    }

    /** Temperatura em °C e código WMO. */
    class Reading(val celsius: Double, val code: Int)

    fun findCities(name: String, lang: String): List<City>? {
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=${URLEncoder.encode(name, "UTF-8")}&count=10&language=$lang"
        val json = get(url) ?: return null
        val results = json.optJSONArray("results") ?: return emptyList()
        return (0 until results.length()).mapNotNull { i ->
            val r = results.optJSONObject(i) ?: return@mapNotNull null
            if (!r.has("latitude") || !r.has("longitude")) return@mapNotNull null
            City(r.optString("name"), r.getDouble("latitude"), r.getDouble("longitude"), r.optString("admin1"), r.optString("country"))
        }.filter { it.name.isNotBlank() }
    }

    fun fetch(lat: String, lon: String): Reading? {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code&timezone=auto"
        val current = get(url)?.optJSONObject("current") ?: return null
        if (!current.has("temperature_2m") || !current.has("weather_code")) return null
        return Reading(current.getDouble("temperature_2m"), current.getInt("weather_code"))
    }

    private fun get(url: String): JSONObject? = try {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        try {
            if (connection.responseCode == HttpURLConnection.HTTP_OK)
                JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            else null
        } finally {
            connection.disconnect()
        }
    } catch (_: Exception) {
        null
    }

    /** O texto da tela inicial ("23° · Nublado"), ou null se está desligado ou nunca carregou. */
    fun text(context: Context, prefs: Prefs): String? {
        if (!prefs.weatherOn || prefs.weatherAt == 0L) return null
        val celsius = prefs.weatherTemp.toDoubleOrNull() ?: return null
        val degrees = (if (prefs.weatherFahrenheit) celsius * 9 / 5 + 32 else celsius).roundToInt()
        return "$degrees° · ${context.getString(condition(prefs.weatherCode))}"
    }

    /** Código WMO -> palavra curta. */
    private fun condition(code: Int): Int = when (code) {
        0, 1 -> R.string.weather_clear
        2 -> R.string.weather_partly_cloudy
        3 -> R.string.weather_cloudy
        45, 48 -> R.string.weather_fog
        in 51..57 -> R.string.weather_drizzle
        in 61..67 -> R.string.weather_rain
        in 71..77, 85, 86 -> R.string.weather_snow
        in 80..82 -> R.string.weather_showers
        in 95..99 -> R.string.weather_thunderstorm
        else -> R.string.weather_cloudy
    }
}
