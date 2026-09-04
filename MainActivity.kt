package de.pvcompact.app

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PVCompactTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
private fun PVCompactTheme(content: @Composable () -> Unit) {
    val light = lightColorScheme(
        primary = Color(0xFF0B7A53),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD6F4E4),
        onPrimaryContainer = Color(0xFF062D20),
        secondary = Color(0xFF3A6C5A),
        background = Color(0xFFF7FAF8),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFE8EFEA)
    )
    val dark = darkColorScheme(
        primary = Color(0xFF73D9AF),
        primaryContainer = Color(0xFF07583C),
        secondary = Color(0xFFA4D2BE),
        background = Color(0xFF111613),
        surface = Color(0xFF18201C),
        surfaceVariant = Color(0xFF26322C)
    )
    MaterialTheme(colorScheme = if (androidx.compose.foundation.isSystemInDarkTheme()) dark else light, content = content)
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val store = remember { CredentialStore(context) }
    var configured by remember { mutableStateOf(store.hasCredentials()) }
    var forecastSettings by remember { mutableStateOf(false) }
    var dashboardRevision by remember { mutableStateOf(0) }

    when {
        !configured -> SetupScreen { sid, key ->
            store.save(sid, key)
            configured = true
        }
        forecastSettings -> ForecastSetupScreen(
            initial = store.getForecastConfig(),
            onSaved = {
                store.saveForecastConfig(it)
                forecastSettings = false
                dashboardRevision++
            },
            onRemove = {
                store.clearForecastConfig()
                forecastSettings = false
                dashboardRevision++
            },
            onCancel = { forecastSettings = false }
        )
        else -> DashboardScreen(
            revision = dashboardRevision,
            onForecastSettings = { forecastSettings = true },
            onReset = {
                store.clear()
                configured = false
            }
        )
    }
}

@Composable
private fun SetupScreen(onSaved: (String, String) -> Unit) {
    var systemId by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text("PV Compact", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(
            "Deine PVOutput-Daten, aufgeräumt und auf einen Blick.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(
            value = systemId,
            onValueChange = { systemId = it.filter(Char::isDigit) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("PVOutput System-ID") },
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it.trim() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("PVOutput API-Key") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Den API-Zugang aktivierst du bei PVOutput unter Settings → API Access. Der Key wird verschlüsselt im Android-Keystore gespeichert.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(22.dp))
        Button(
            onClick = { onSaved(systemId, apiKey) },
            enabled = systemId.isNotBlank() && apiKey.length >= 8,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Verbinden")
        }
    }
}

@Composable
private fun ForecastSetupScreen(
    initial: ForecastConfig?,
    onSaved: (ForecastConfig) -> Unit,
    onRemove: () -> Unit,
    onCancel: () -> Unit
) {
    var latitude by remember { mutableStateOf(initial?.latitude?.toString() ?: "") }
    var longitude by remember { mutableStateOf(initial?.longitude?.toString() ?: "") }
    var kwp by remember { mutableStateOf(initial?.kwp?.toString() ?: "") }
    var tilt by remember { mutableStateOf((initial?.tilt ?: 30).toString()) }
    var azimuth by remember { mutableStateOf((initial?.azimuth ?: 0).toString()) }
    var performance by remember { mutableStateOf(((initial?.performanceRatio ?: 0.85) * 100.0).toInt().toString()) }
    var forecastKey by remember { mutableStateOf(initial?.forecastSolarApiKey ?: "") }

    fun number(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()
    val lat = number(latitude)
    val lon = number(longitude)
    val power = number(kwp)
    val tiltValue = tilt.toIntOrNull()
    val azimuthValue = azimuth.toIntOrNull()
    val performanceValue = number(performance)
    val valid = lat != null && lat in -90.0..90.0 && lon != null && lon in -180.0..180.0 &&
        power != null && power > 0 && tiltValue != null && tiltValue in 0..90 &&
        azimuthValue != null && azimuthValue in -180..180 && performanceValue != null && performanceValue in 50.0..100.0

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("PV-Forecast", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Standort und Anlagendaten", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onCancel) { Text("Zurück") }
        }
        Spacer(Modifier.height(20.dp))

        SectionCard("Standort") {
            Text(
                "Breiten- und Längengrad deiner PV-Anlage. Dezimalwerte reichen aus.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(latitude, { latitude = it }, Modifier.fillMaxWidth(), label = { Text("Breitengrad, z. B. 50.12345") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(longitude, { longitude = it }, Modifier.fillMaxWidth(), label = { Text("Längengrad, z. B. 8.12345") }, singleLine = true)
        }

        Spacer(Modifier.height(14.dp))
        SectionCard("PV-Anlage") {
            OutlinedTextField(kwp, { kwp = it }, Modifier.fillMaxWidth(), label = { Text("Anlagengröße in kWp") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(tilt, { tilt = it.filter { ch -> ch.isDigit() } }, Modifier.fillMaxWidth(), label = { Text("Dachneigung 0–90°") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(azimuth, { azimuth = it.filter { ch -> ch.isDigit() || ch == '-' } }, Modifier.fillMaxWidth(), label = { Text("Ausrichtung: Ost −90 · Süd 0 · West 90") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(performance, { performance = it.filter { ch -> ch.isDigit() || ch == ',' || ch == '.' } }, Modifier.fillMaxWidth(), label = { Text("Anlagenfaktor in %, Standard 85") }, singleLine = true)
            Spacer(Modifier.height(6.dp))
            Text(
                "Der Anlagenfaktor berücksichtigt typische Verluste von Modulen, Wechselrichter, Temperatur und Kabeln. Er betrifft nur den kostenlosen Open-Meteo-Fallback.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(14.dp))
        SectionCard("Forecast.Solar · optional") {
            Text(
                "Ohne Key nutzt PV Compact Open-Meteo. Mit einem persönlichen Forecast.Solar-Key wird dessen direktes PV-Modell verwendet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                forecastKey,
                { forecastKey = it.trim() },
                Modifier.fillMaxWidth(),
                label = { Text("Forecast.Solar API-Key (optional)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
        }

        Spacer(Modifier.height(18.dp))
        Button(
            onClick = {
                onSaved(
                    ForecastConfig(
                        latitude = lat!!,
                        longitude = lon!!,
                        tilt = tiltValue!!,
                        azimuth = azimuthValue!!,
                        kwp = power!!,
                        performanceRatio = performanceValue!! / 100.0,
                        forecastSolarApiKey = forecastKey
                    )
                )
            },
            enabled = valid,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("Forecast speichern") }

        if (initial != null) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onRemove, modifier = Modifier.fillMaxWidth()) { Text("Forecast-Konfiguration entfernen") }
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun DashboardScreen(revision: Int, onForecastSettings: () -> Unit, onReset: () -> Unit) {
    val context = LocalContext.current
    val store = remember { CredentialStore(context) }
    var data by remember { mutableStateOf<DashboardData?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        if (loading) return
        loading = true
        error = null
        Thread {
            try {
                val baseResult = PvOutputApi(store.getApiKey(), store.getSystemId()).loadDashboard()
                var forecast: ForecastData? = null
                var forecastError: String? = null
                store.getForecastConfig()?.let { config ->
                    try {
                        forecast = PvForecastApi(config).load()
                    } catch (e: Exception) {
                        forecastError = e.message ?: "Forecast konnte nicht geladen werden"
                    }
                }
                val result = baseResult.copy(forecast = forecast, forecastError = forecastError)
                WidgetCache.save(context, result.live, forecast)
                Handler(Looper.getMainLooper()).post {
                    data = result
                    loading = false
                    val manager = AppWidgetManager.getInstance(context)
                    val ids = manager.getAppWidgetIds(ComponentName(context, PvWidgetProvider::class.java))
                    if (ids.isNotEmpty()) {
                        val intent = android.content.Intent(context, PvWidgetProvider::class.java).apply {
                            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                        }
                        context.sendBroadcast(intent)
                    }
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    error = e.message ?: "Unbekannter Fehler"
                    loading = false
                }
            }
        }.start()
    }

    LaunchedEffect(revision) { refresh() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("PV Compact", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Meine Anlage", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onForecastSettings) { Text("Forecast") }
            TextButton(onClick = onReset) { Text("Zugang") }
            Button(onClick = { refresh() }, enabled = !loading) { Text(if (loading) "…" else "↻") }
        }

        Spacer(Modifier.height(18.dp))

        if (error != null) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text("PVOutput konnte nicht geladen werden", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(error ?: "")
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        val current = data
        if (current == null && loading) {
            Box(Modifier.fillMaxWidth().padding(vertical = 70.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (current != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Heute", formatKwh(current.live.energyWh), "Erzeugung", Modifier.weight(1f))
                MetricCard("Aktuell", formatKw(current.live.powerW), "PV-Leistung", Modifier.weight(1f))
            }

            val consumption = current.live.consumptionWh
            if (consumption != null && consumption > 0.0) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard("Verbrauch", formatKwh(consumption), "heute", Modifier.weight(1f))
                    MetricCard("Netz / Haus", current.live.consumptionW?.let(::formatKw) ?: "–", "aktuell", Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(18.dp))
            ForecastSection(current, store.getForecastConfig() != null, onForecastSettings)

            Spacer(Modifier.height(16.dp))
            SectionCard("Tagesverlauf") {
                PowerChart(current.history, Modifier.fillMaxWidth().height(210.dp))
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("00:00", style = MaterialTheme.typography.labelSmall)
                    Text("12:00", style = MaterialTheme.typography.labelSmall)
                    Text("24:00", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionCard("Letzte 7 Tage") {
                WeekChart(current.week, Modifier.fillMaxWidth().height(220.dp))
                Spacer(Modifier.height(10.dp))
                val total = current.week.sumOf { it.generatedWh }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Wochensumme", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatKwh(total), fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionCard("Status") {
                StatusRow("Letzter PVOutput-Wert", "${prettyDate(current.live.date)} · ${current.live.time} Uhr")
                current.live.temperatureC?.let { StatusRow("Temperatur", String.format(Locale.GERMANY, "%.1f °C", it)) }
                current.live.voltageV?.let { StatusRow("Spannung", String.format(Locale.GERMANY, "%.1f V", it)) }
                current.rateRemaining?.let { StatusRow("PVOutput API-Abrufe übrig", "$it in dieser Stunde") }
                current.forecast?.let { StatusRow("Forecast-Quelle", it.source) }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun ForecastSection(current: DashboardData, configured: Boolean, onSettings: () -> Unit) {
    if (!configured) {
        SectionCard("PV-Forecast · nächste 3 Tage") {
            Text(
                "Standort, kWp, Dachneigung und Ausrichtung hinterlegen – dann zeigt PV Compact die wetterabhängige Ertragsprognose.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onSettings) { Text("Forecast einrichten") }
        }
        return
    }

    val forecast = current.forecast
    if (forecast == null) {
        SectionCard("PV-Forecast · nächste 3 Tage") {
            Text(
                current.forecastError ?: "Noch keine Prognosedaten verfügbar.",
                color = if (current.forecastError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onSettings) { Text("Forecast-Einstellungen") }
        }
        return
    }

    val todayIso = LocalDate.now().toString()
    val todayForecast = forecast.days.firstOrNull { it.date == todayIso }
    val future = forecast.days.filter { runCatching { LocalDate.parse(it.date).isAfter(LocalDate.now()) }.getOrDefault(false) }.take(3)

    SectionCard("PV-Forecast · nächste 3 Tage") {
        if (todayForecast != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("Prognose heute", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatKwh(todayForecast.energyWh), fontSize = 25.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Bisher erzeugt", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatKwh(current.live.energyWh), fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(14.dp))
            HorizontalDivider()
            Spacer(Modifier.height(6.dp))
        }

        if (future.isEmpty()) {
            Text("Die Forecast-Quelle hat derzeit keine weiteren Tage geliefert.")
        } else {
            future.forEach { ForecastDayRow(it) }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            buildString {
                append("Quelle: ${forecast.source}")
                forecast.note?.let { append(" · $it") }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onSettings, contentPadding = PaddingValues(0.dp)) { Text("Forecast-Einstellungen") }
    }
}

@Composable
private fun ForecastDayRow(day: ForecastDay) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(forecastDayLabel(day.date), fontWeight = FontWeight.SemiBold)
            val detail = buildList<String> {
                weatherLabel(day.weatherCode)?.let { add(it) }
                if (day.temperatureMinC != null && day.temperatureMaxC != null) {
                    add(String.format(Locale.GERMANY, "%.0f–%.0f °C", day.temperatureMinC, day.temperatureMaxC))
                }
            }.joinToString(" · ")
            if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(formatKwh(day.energyWh), fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MetricCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .72f))
            Spacer(Modifier.height(5.dp))
            Text(value, fontSize = 27.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .72f))
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun PowerChart(points: List<HistoryPoint>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val safe = points.filter { it.powerW >= 0 }
    Canvas(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .20f), RoundedCornerShape(14.dp)).padding(8.dp)) {
        val left = 46f
        val top = 14f
        val right = size.width - 8f
        val bottom = size.height - 22f
        repeat(4) { i ->
            val y = top + (bottom - top) * i / 3f
            drawLine(gridColor, Offset(left, y), Offset(right, y), 1f)
        }
        if (safe.isEmpty()) return@Canvas
        val maxW = max(1000.0, safe.maxOf { it.powerW })
        val path = Path()
        safe.forEachIndexed { index, p ->
            val minutes = timeToMinutes(p.time).coerceIn(0, 1440)
            val x = left + (right - left) * (minutes / 1440f)
            val y = bottom - (bottom - top) * (p.powerW / maxW).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, lineColor, style = Stroke(width = 4f))
        drawContext.canvas.nativeCanvas.apply {
            val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 24f
                color = labelColor.toArgbCompat()
            }
            drawText(String.format(Locale.GERMANY, "%.1f kW", maxW / 1000.0), 4f, top + 8f, textPaint)
            drawText("0", 20f, bottom, textPaint)
        }
    }
}

@Composable
private fun WeekChart(days: List<DailyOutput>, modifier: Modifier = Modifier) {
    val barColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier) {
        if (days.isEmpty()) return@Canvas
        val maxWh = max(1000.0, days.maxOf { it.generatedWh })
        val chartBottom = size.height - 34f
        val chartTop = 8f
        drawLine(gridColor, Offset(0f, chartBottom), Offset(size.width, chartBottom), 1f)
        val slot = size.width / days.size
        days.forEachIndexed { i, d ->
            val height = (chartBottom - chartTop) * (d.generatedWh / maxWh).toFloat()
            val x = slot * i + slot * .22f
            val w = slot * .56f
            drawRoundRect(barColor, topLeft = Offset(x, chartBottom - height), size = androidx.compose.ui.geometry.Size(w, height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f))
            val date = runCatching { LocalDate.parse(d.date, DateTimeFormatter.BASIC_ISO_DATE) }.getOrNull()
            val label = date?.dayOfWeek?.getDisplayName(TextStyle.SHORT, Locale.GERMAN)?.take(2) ?: ""
            drawContext.canvas.nativeCanvas.apply {
                val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 24f
                    color = textColor.toArgbCompat()
                }
                drawText(label, x + w / 2f, size.height - 6f, textPaint)
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

private fun timeToMinutes(time: String): Int {
    val p = time.split(':')
    return (p.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (p.getOrNull(1)?.toIntOrNull() ?: 0)
}

private fun forecastDayLabel(raw: String): String = runCatching {
    val date = LocalDate.parse(raw)
    val prefix = when (date) {
        LocalDate.now().plusDays(1) -> "Morgen"
        LocalDate.now().plusDays(2) -> "Übermorgen"
        else -> date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.GERMAN)
    }
    "$prefix · ${date.format(DateTimeFormatter.ofPattern("dd.MM."))}"
}.getOrDefault(raw)

private fun weatherLabel(code: Int?): String? = when (code) {
    null -> null
    0 -> "Sonnig"
    1 -> "Überwiegend sonnig"
    2 -> "Teilweise bewölkt"
    3 -> "Bewölkt"
    45, 48 -> "Nebel"
    in 51..57 -> "Nieselregen"
    in 61..67 -> "Regen"
    in 71..77 -> "Schnee"
    in 80..82 -> "Schauer"
    in 85..86 -> "Schneeschauer"
    in 95..99 -> "Gewitter"
    else -> "Wettercode $code"
}

private fun formatKwh(wh: Double): String = String.format(Locale.GERMANY, "%.2f kWh", wh / 1000.0)
private fun formatKw(w: Double): String = String.format(Locale.GERMANY, "%.2f kW", w / 1000.0)
private fun prettyDate(raw: String): String = runCatching {
    LocalDate.parse(raw, DateTimeFormatter.BASIC_ISO_DATE).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
}.getOrDefault(raw)
private fun Color.toArgbCompat(): Int = android.graphics.Color.argb((alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())
