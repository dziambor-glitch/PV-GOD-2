# PV Compact als APK bauen – einfache Anleitung

Du brauchst keine Programmierkenntnisse. Am einfachsten ist Android Studio.

## Weg A – Android Studio auf Windows/Mac/Linux

1. Android Studio von https://developer.android.com/studio installieren.
2. Die ZIP-Datei dieses Projekts entpacken.
3. Android Studio starten und **Open** wählen.
4. Den Ordner **PVCompact** auswählen.
5. Beim ersten Öffnen die vorgeschlagenen Android-SDK-Komponenten installieren lassen.
6. Warten, bis der Gradle-Sync abgeschlossen ist.
7. Oben im Menü wählen: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
8. Nach erfolgreichem Build auf **locate** klicken oder diesen Ordner öffnen:
   `app/build/outputs/apk/debug/`
9. Die Datei `app-debug.apk` auf das Android-Handy kopieren.
10. Auf dem Handy die APK antippen. Falls Android fragt, die Installation aus dieser Quelle für den verwendeten Dateimanager/Browser erlauben.

Die Debug-APK ist bereits automatisch signiert und kann auf einem eigenen Gerät getestet werden.

## Weg B – GitHub baut die APK automatisch

Im Projekt liegt bereits `.github/workflows/build-apk.yml`.

1. Kostenloses GitHub-Konto anlegen/anmelden.
2. Neues Repository erstellen.
3. Den Inhalt des Ordners **PVCompact** in das Repository hochladen.
4. In GitHub oben **Actions** öffnen.
5. Workflow **APK bauen** auswählen.
6. **Run workflow** anklicken.
7. Nach erfolgreichem Lauf unten unter **Artifacts** auf **PVCompact-debug-APK** klicken.
8. ZIP herunterladen und entpacken. Darin liegt `app-debug.apk`.

## Später: richtige Release-APK

Für eine dauerhaft verteilbare Version wird eine eigene Signatur (Keystore) angelegt. Dies ist besonders wichtig, damit spätere Updates als Update derselben App installiert werden können. Den privaten Keystore niemals öffentlich hochladen oder weitergeben.
