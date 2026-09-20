# Vectrum
So Leude, Forza ist 'runtergeladen

Minecraft-Mod für **Fabric**, **Forge** und **NeoForge** aus einer gemeinsamen Codebasis.
Aktuell unterstützt: **Minecraft 1.20.1** (Java 17). Weitere Versionen sind vorbereitet, siehe unten.

## Aufbau

Das Projekt nutzt [Stonecutter](https://stonecutter.kikugie.dev) (mehrere Minecraft-Versionen aus einem Quelltext)
zusammen mit Architectury Loom (Fabric, Forge und NeoForge im selben Build). Jede Kombination aus
Version und Loader ist ein eigenes Gradle-Projekt, z. B. `1.20.1-fabric`.

```
settings.gradle.kts           Liste aller Versions-/Loader-Projekte
stonecutter.gradle.kts        Steuer-Skript (aktive Version, Loader-Konstanten, buildAll)
build.gradle.kts              Ein Build-Skript für ALLE Projekte (verzweigt nach Loader)
gradle.properties             Mod-Metadaten (ID, Name, Version, Autor, Lizenz)
versions/<mc>-<loader>/       Versionen der Abhängigkeiten je Projekt (Loader, API, Parchment, pack_format)
src/main/java/.../Vectrum.java          gemeinsamer Einstieg
src/main/java/.../registry/             loaderunabhängige Registrierung (Blöcke, Items, Creative-Tab)
src/main/java/.../datagen/              loaderunabhängige Datengenerierung (nur Vanilla-Klassen)
src/main/java/.../core/                 Netzwerk-Kern OHNE Minecraft-Klassen (Graph, Transportarten, Hilfsmathe)
src/test/java/.../core/                 automatische Tests für den Kern
docs/                                   Konzept, Implementierungs-Prompt, Entscheidungsliste
src/main/java/.../block/                Kabel, Endpunkt (gemeinsame Basis ConduitBlock), Verbindungszustände, Formen
src/main/java/.../world/                Netzwerke einer Dimension (Speicherung, Verbindung zum Kern)
src/main/java/.../logistics/            Transportlogik (wer liefert wohin)
src/main/java/.../command/              Verwaltungsbefehle (/vectrum ...), loaderunabhängig
src/main/java/.../transfer/             Zugang zu Speichern (Items, Fluide, Energie), gemeinsame Schnittstelle (Umsetzung je Loader)
src/main/java/.../item/                 Werkzeuge (Wrench, Diagnosewerkzeug)
src/main/resources/assets/vectrum/      Handgeschriebene Modelle und Blockstates von Kabel und Endpunkt, Texturen
src/main/java/.../platform/fabric/      Fabric-Einstieg + Fabric-Datagen-Einstieg + Inventarzugriff (Transfer API)
src/main/java/.../platform/forge/       Forge-Einstieg + Inventarzugriff (Item-Handler), nutzt auch NeoForge 1.20.1
platforms/<loader>/resources/           fabric.mod.json bzw. META-INF/mods.toml
generated/<mc>/                         Ausgabe des Datagens (wird als Ressourcenordner eingebunden)
```

Forge und NeoForge teilen sich bis 1.20.1 dieselbe API (`net.minecraftforge.*`) und damit dasselbe Paket
`platform/forge`. Für NeoForge 1.20.2 und neuer wird ein eigenes Paket `platform/neoforge` erwartet; die
Auswahl steht schon in `build.gradle.kts` (`sourcePlatform`).

## Loaderspezifischer Code

Unterschiede zwischen Loadern und Versionen werden mit Stonecutter-Kommentaren direkt im Code geschrieben,
Beispiele stehen in `Vectrum.java` und `ModCreativeTabs.java`:

```java
//? if fabric {
return "fabric";
//?} else if neoforge {
/*return "neoforge";
*///?} else {
/*return "forge";
*///?}
```

Welche Variante gerade im Editor aktiv ist, steht in `stonecutter.gradle.kts` (`stonecutter active ...`).
In IntelliJ lässt sie sich über die Gradle-Tasks `Set active project to ...` (Gruppe *stonecutter*) umschalten.

## Häufige Befehle

| Befehl | Wirkung |
| --- | --- |
| `./gradlew :1.20.1-fabric:runClient` | Client mit Fabric starten (analog `-forge`, `-neoforge`) |
| `./gradlew :1.20.1-fabric:runDatagen` | Datagen über Fabric ausführen, schreibt nach `generated/1.20.1/` |
| `./gradlew :1.20.1-forge:runData` | Datagen über Forge (oder `-neoforge`) |
| `./gradlew :1.20.1-forge:build` | Nur diesen Loader bauen (Jar in `versions/1.20.1-forge/build/libs`) |
| `./gradlew :1.20.1-forge:test` | Automatische Tests des Kerns ausführen |
| `./gradlew buildAll` | Alle Loader und Versionen bauen |

Unter Windows `gradlew.bat` statt `./gradlew` verwenden. Beim ersten Start lädt Loom Minecraft und die
Mappings herunter, das dauert einige Minuten.

## Kern und Tests

Im Paket `core` liegt die Logik, die kein Minecraft braucht: der Netzwerk-Graph (Kabel und Endpunkte, Verschmelzen
und Teilen von Netzen), die Transportarten, die Durchsatzlimits und die Hilfsmathe. Weil der Kern ohne Minecraft läuft, lässt er sich
mit normalen JUnit-Tests prüfen (`src/test/java`), auch mit Zufallstests gegen eine einfache Referenzlösung und
Leistungsmessungen. Der Test `CoreIsolationTest` schlägt fehl, sobald jemand im Kern Minecraft- oder Loader-Klassen
importiert. Die Tests laufen mit jedem Loader-Projekt, z. B. `./gradlew :1.20.1-forge:test`.

Konzept, Aufgabenstellung und getroffene Entscheidungen stehen im Ordner `docs/`.

## Datagen

`generated/1.20.1/` enthält Lang-Dateien (en_us, de_de), Modelle, Blockstate, Loot-Table, Rezepte und Block-Tags
für den Beispielblock. Die Dateien wurden anfangs von Hand vorbefüllt, damit die Mod sofort spielbar ist; ein
Datagen-Lauf ersetzt sie durch die echte Ausgabe (und ergänzt z. B. die Rezept-Advancements). Die Provider liegen
in `datagen/` und laufen auf allen drei Loadern unverändert.

Die Texturen (`src/main/resources/assets/vectrum/textures/`) sind einfache Platzhalter.

## Neue Minecraft-Version hinzufügen (Beispiel 1.21.1)

1. In `settings.gradle.kts` eine Zeile ergänzen, z. B. `match("1.21.1", "fabric", "neoforge")`.
2. Je Loader `versions/1.21.1-<loader>/gradle.properties` anlegen (Vorlage: die Dateien von 1.20.1) und
   `java_version` (21 ab 1.20.5), `pack_format`, Loader-/API-Versionen sowie `parchment_version` anpassen.
3. Bei NeoForge ab 1.20.2 zusätzlich `loom.platform=neoforge` setzen, Paket `platform/neoforge` anlegen und
   die Metadaten-Datei in `platforms/neoforge/resources/` anpassen (ab 1.20.5 heißt sie `neoforge.mods.toml`).
4. Unterschiede im Code per Stonecutter-Kommentar lösen, z. B. `//? if >=1.21 { ... //?}`.
5. Mit `Set active project to 1.21.1-...` die neue Version im Editor aktivieren und mit `buildAll` prüfen.

## Mitgelieferte Software

- Fabric-Jar: [Team Reborn Energy](https://github.com/TechReborn/Energy) 3.0.0 (MIT-Lizenz), per Jar-in-Jar für die Energie-Übertragung.

## Bekannte Lücken

- Es sind noch keine Mixins enthalten. Zum Ergänzen: `vectrum.mixins.json` anlegen, in `fabric.mod.json` unter
  `mixins` eintragen und für Forge/NeoForge in `build.gradle.kts` `loom { forge { mixinConfig("vectrum.mixins.json") } }` setzen.
- Lizenz: `All Rights Reserved` (siehe `LICENSE`, gleiche Angabe in den Mod-Metadaten).
