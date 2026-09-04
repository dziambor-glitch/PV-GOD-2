# Build Notes – PV Compact 0.2

## Neu in 0.2

- Forecast-Konfiguration gespeichert in eigenen SharedPreferences
- Forecast.Solar-Key verschlüsselt über denselben Android-Keystore-Schlüssel wie der PVOutput-Key
- `PvForecastApi.kt` mit Forecast.Solar + automatischem Open-Meteo-Fallback
- Open-Meteo-GTI wird mit kWp und Performance Ratio in geschätzte Tagesenergie umgerechnet
- Dashboard: Prognose heute und nächste 3 Tage
- Widget: zusätzliche Prognose für morgen

## Prüfung

Die reinen Kotlin-Modelle und `PvForecastApi.kt` wurden mit `kotlinc` gegen JSON-Stubs auf Syntax/Typkonsistenz geprüft. In dieser Umgebung ist weiterhin kein vollständiges Android SDK/Gradle-Setup vorhanden, daher konnte kein echtes APK gebaut oder ein Instrumentation-Test ausgeführt werden.

## Vor Release testen

- Android-Studio-Sync mit AGP 9.4 / Gradle 9.6
- Open-Meteo-Abruf auf realem Gerät
- Forecast.Solar mit Personal-Key
- Widget-Refresh unter Android 12–16
- Dark Mode
- Dezimalkomma bei Standort/kWp
