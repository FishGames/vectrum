# Entscheidungen zur Umsetzung

Ergänzt `konzept.md` und `implementierungs-prompt.md`. Bei Widersprüchen gilt diese Liste, weil sie später und
konkreter ist. Stand: 20.09.2026.

## Vom Projektinhaber festgelegt

| Nr. | Entscheidung |
| --- | --- |
| P1 | Der Mod ist öffentlich und für Modpacks gedacht. Er muss auf Fabric, Forge **und** NeoForge laufen. Der Name "Vectrum" ist frei. |
| P2 | Keine Mods als Voraussetzung. Erlaubt ist nur die Fabric API. Alles andere ist eine optionale Anbindung. |
| P3 | Texturen und Sounds liefert der Projektinhaber. Modelle entstehen zunächst durch Claude (Quader-Formen) und werden bei Bedarf von Hand angepasst. |
| P4 | Ein **Endpunkt** ist ein pipe-artiger Block. Er verbindet sich wie bekannte Pipes mit umliegenden Inventaren, theoretisch mit allen 6 Seiten. |
| P5 | **Digitales Netz (Stufe 3)** überbrückt keine Dimensionen, es ist kabelbasiert. |
| P6 | **Kabellos (Stufe 4)** darf mit den nötigen Upgrades Dimensionen überbrücken. |
| P7 | Der Coder bildet ein Netz aus Teilnehmern. Damit das auch bei großen Bauten übersichtlich bleibt, entscheidet Claude über die Umsetzung (siehe E3). |

## Von Claude getroffen (jederzeit änderbar)

| Nr. | Entscheidung | Begründung |
| --- | --- | --- |
| E1 | **Durchsatzlimit:** Nur der Endpunkt bestimmt es über seine Durchsatz-Upgrades. Kabel bremsen nicht. Das Limit ist ein gespeicherter Wert (K3). Ein festes Grundlimit pro Kabelart lässt sich später ergänzen, ohne den Aufbau zu ändern. | Kabel haben keine Daten und keine Upgrades. Ein Limit "der Strecke" müsste sonst pro Übergabe über den Pfad berechnet werden. |
| E2 | **Endpunkte leiten auch:** Ein Endpunkt ist ein Netzknoten wie ein Kabel und verbindet sich mit angrenzenden Kabeln und Endpunkten desselben Typs. Rolle (Quelle/Ziel), Filter, Priorität und Modus werden **pro angeschlossener Seite** eingestellt. Upgrades gehören zum Endpunkt-Block. | Pipe-Verhalten wie bei bekannten Mods; die Bedienung bleibt ein GUI mit Seitenauswahl. |
| E3 | **Übersicht im digitalen Netz:** (1) Coder haben einen frei wählbaren Namen. (2) Coder lassen sich mit Farbstoff einfärben. (3) Das Wrench zeigt per Klick auf einen Coder oder ein digitales Kabel eine Netzübersicht: Liste aller Coder mit Name, Farbe, Ort, Status und aktuellem Fluss. Es zeigt keine Inhalte, damit es kein Terminal wird. (4) Mit dem Wrench in der Hand wird das ganze Netz umrissen. | Ein einziges Werkzeug statt eines neuen Blocks; hält die Regel "eine Funktion, höchstens ein Block" ein und verletzt das Verbot von Storage/Terminal nicht. Umsetzung in Etappe 9 und 12. |
| E4 | **Stufe 4 bekommt Upgrade-Plätze**, mindestens für ein neues **Dimensions-Upgrade**. Konzept-Änderung: Die Upgrade-Tabelle wird um "Dimensionen" ergänzt. Digitale Kabel bleiben ohne Upgrades. | Folgt aus P6. |
| E5 | **Stufe-4-Durchsatz:** standardmäßig unbegrenzt, über einen Konfigurationswert umschaltbar (wie im Prompt gefordert). | Ein Limit nachträglich einzuführen ist einfacher, als es wieder zu entfernen. |
| E6 | **Hauptumgebung Forge.** Forge und NeoForge teilen sich in 1.20.1 einen Anschluss (`platform/forge`), Fabric hat einen eigenen. Für Energie auf Fabric wird Team Reborn Energy eingebaut (Jar-in-Jar), die Lizenz ist vorher zu prüfen. | Die Wunschmods (Mekanism, Create, Refined Storage, ...) laufen unter 1.20.1 überwiegend auf Forge. |
| E7 | **Kern ohne Minecraft:** Alles unter `io.github.fishgames.vectrum.core` importiert nichts aus Minecraft, Forge, Fabric oder Mojang-Bibliotheken. Ein Test prüft das automatisch. | Der Kern soll ohne laufendes Minecraft testbar sein. |
| E8 | **Koordinaten enthalten die Dimension** (`BlockCoord`). Das drahtlose Frequenzregister (Stufe 4) wird später serverweit geführt, alle anderen Netze pro Dimension. | Hält P6 offen, ohne den Kern später umbauen zu müssen. |
| E9 | **Redstone** wird als Transporttyp mit anderem Verhalten (`SIGNAL`: Wert spiegeln) angelegt, alle anderen Typen als `QUANTITY` (Menge übergeben). | Vermeidet Sonderfallcode im Kern, sobald Redstone dazukommt. |

## Noch offen

- Lizenz von Team Reborn Energy prüfen (E6).
- Verfügbarkeit von JEI, EMI und Jade/WTHIT/TOP für alle drei Loader in 1.20.1 prüfen.
- Ob Mekanism für NeoForge 1.20.1 existiert (Gas-Modul), vor Etappe 11 klären.
