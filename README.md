# PV Compact 0.2

Schlanke Android-App als moderne Anzeige für ein PVOutput-System – jetzt zusätzlich mit wetterabhängigem PV-Forecast.

## Funktionen

- Heute: Erzeugung in kWh und aktuelle Leistung in kW
- Tageskurve aus PVOutput Live-History
- Letzte 7 Tage als Balkendiagramm + Wochensumme
- Optional Verbrauch/Verbrauchsleistung, wenn PVOutput diese Werte liefert
- PV-Forecast: Prognose heute + nächste 3 Tage
- Forecast-Konfiguration mit Standort, kWp, Dachneigung und Ausrichtung
- Wetterhinweise und Min/Max-Temperatur im Forecast (bei Open-Meteo)
- Homescreen-Widget mit heutigem Ertrag, aktueller Leistung und Prognose für morgen
- Widget-Update alle 30 Minuten
- PVOutput API-Rate-Limit-Anzeige
- PVOutput- und Forecast.Solar-API-Key werden per Android Keystore AES/GCM verschlüsselt gespeichert
- Reine Lese-App: keine Messwerte werden zu PVOutput geschrieben

## Forecast-Quellen

### Open-Meteo – Standard, ohne API-Key

Wenn kein Forecast.Solar-Key hinterlegt ist, lädt die App `global_tilted_irradiance` (Einstrahlung auf die geneigte Modulfläche) aus Open-Meteo und berechnet daraus die erwartete PV-Energie anhand von:

- Breitengrad / Längengrad
- Dachneigung
- Ausrichtung / Azimut
- installierter Leistung in kWp
- Anlagenfaktor / Performance Ratio (Standard 85 %)

Azimut in der App: Ost = -90°, Süd = 0°, West = +90°.

### Forecast.Solar – optional

Wird ein eigener Forecast.Solar-API-Key eingetragen, versucht die App zuerst Forecast.Solar zu verwenden. Falls dieser Abruf fehlschlägt, fällt die App automatisch auf Open-Meteo zurück.

Für einen Forecast bis einschließlich der nächsten 3 Tage ist bei Forecast.Solar nach aktuellem Tarifmodell der Personal-Zugang oder höher nötig. Der Key gehört dem Nutzer und wird nicht mit der App ausgeliefert.

## PVOutput Zugang

In PVOutput: Settings → API Access aktivieren → API Key erzeugen → System Id notieren.
Beim ersten App-Start werden System-ID und API-Key abgefragt.

## Forecast einrichten

Im Dashboard oben auf `Forecast` tippen und eintragen:

1. Breitengrad und Längengrad der Anlage
2. Anlagengröße in kWp
3. Dachneigung in Grad
4. Ausrichtung: Ost -90, Süd 0, West +90
5. Anlagenfaktor (Standard 85 %)
6. optional: eigener Forecast.Solar API-Key

## Build

Voraussetzungen laut aktuellem Projektstand:
- Android Studio mit JDK 17
- Android SDK API 37
- Android Gradle Plugin 9.4 / Gradle 9.6

Projekt in Android Studio öffnen und `Build > Build APK(s)` ausführen.

## Wichtiger Hinweis

Dieses Projekt ist ein unabhängiger Client für die dokumentierte PVOutput API, Forecast.Solar und Open-Meteo. Es ist nicht von PVOutput.org, Forecast.Solar, Open-Meteo oder PVsolcast herausgegeben.

## APK bauen

Für Einsteiger liegt eine Schritt-für-Schritt-Anleitung in `APK_BAUEN_EINFACH.md`. Außerdem ist ein GitHub-Actions-Workflow unter `.github/workflows/build-apk.yml` enthalten, der automatisch eine Debug-APK erzeugt.
