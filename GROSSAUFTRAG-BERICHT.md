# Großauftrag: Bericht

Temporäre Datei zum Durchlesen; nach dem Lesen löschen (ich kann Dateien auf deinem Rechner nicht löschen).

Stand: Verbesserungen 2 bis 9, Konfigurationsdatei, Schwächenprüfung und Konkurrenzvergleich sind umgesetzt bzw. bewertet. Alle drei Loader kompilieren, die Unit-Tests laufen. Der Absturztest steht am Ende dieses Berichts. Performance wurde wie angeordnet nicht gemessen.

## 1. Umgesetzt

| Nr. | Verbesserung | Wo |
| --- | --- | --- |
| 2 | Quelle ohne passende Ware wird getrennt von „Ziele voll“ behandelt: `Port.canGive` prüft vor jeder Zielabfrage, ob die Quelle überhaupt etwas hat, das beide Filter durchlassen. Leere Quellen fragen kein Ziel mehr und schicken keines schlafen. Quellen, die 30 Ticks lang nichts bewegt haben, warten länger (bis `idle_interval_max`). | `Transport`, `LevelNetworks.nextDelay`, alle `*Port.canGive` |
| 3 | Ablehnungs-Cache: der Forge-Item-Port liest die belegten Slots einmal pro Runde, merkt sich abgelehnte Stacks und Filterurteile pro Item während einer Bewegung; Ziele, die Ware ablehnen, die die Quelle wirklich hat, pausieren nach der Tabelle `target_pause`. | `ForgeItemPort`, `LevelNetworks.noteMiss` |
| 4 | Epoche eingeengt: Caches, Back-offs und Flussdaten liegen pro Schicht (Transporttyp), nicht mehr global. Eine Änderung räumt nur ihre Schicht (Digital-Schicht und Coder-Frequenzen: alle). Wireless-Empfängerlisten (dimensionsübergreifend) prüfen einen globalen Zähler `LevelNetworks.stamp()`; `WirelessBlock.neighborChanged` räumt nur noch, wenn sich die Seiten mit Lager geändert haben. | `LevelNetworks`, `WirelessRegistry`, `WirelessBlock` |
| 5 | Weniger Port-Zugriffe: Kabel prüfen ihre Verbindungen nur noch alle `refresh_interval` Ticks (100) und sofort bei jedem Nachbar-Update, nicht mehr bei jedem Lauf. Ziel-Ports werden pro Runde nur einmal gesucht. | `ConduitBlock.tick` |
| 6 | Prioritätsgruppen werden einmal pro Zielliste berechnet; abgeleitete Werte pro Block (Upgrades, Intervall, Typenzahl, wirksame Einstellungen, Filter pro Typ) sind pro Block gecacht; Durchfluss-/Back-off-Daten sind veränderbare Objekte statt neuer Records und Schlüssel; Hot-Path ohne HashSets, TreeMaps, Lambdas pro Ziel; `Target` hat eigenes `equals`/`hashCode` und den typbezogenen Filter eingebaut. | `Transport.Round`, `LevelNetworks.SourceInfo`, `Router.groupByPriority` |
| 7 | Arbeitsbudget pro Server-Tick (`time_budget_micros`, 10 ms) für Kabel und Wireless; Übertragungen über dem Limit laufen im nächsten Tick weiter (hohe Priorität, Reihenfolge bleibt). Dazu eine Zeitscheibe pro Durchlauf eines Kabels (`run_budget_micros`, 2 ms): ein Kabel mit riesigem Durchsatz bricht sich selbst ab und schafft den Rest im nächsten Durchlauf, statt andere Kabel zu verdrängen. | `WorkBudget`, `RunLimit` |
| 8 | Speicherung: Round-Robin-Zeiger machen die Daten nicht mehr „dirty“; Einstellungs- und Zeigertabellen sind pro Block indiziert (Block entfernen scannt nicht mehr alles); Durchsatz wird abgeleitet und nicht mehr gespeichert (alte Welten werden einmalig bereinigt). | `PortSettingsTable`, `RoundRobinPointers`, `LevelNetworks` |
| 9 | Entscheidung: kein asynchroner Snapshot/Plan/Commit (siehe E104), Port-Caching über Ticks hinweg nicht (E105). | `docs/decisions.md` |

Zusätzlich: Startversatz pro Block (E106), damit zusammen gesetzte Quellen nicht alle im selben Tick laufen.

## 2. Konfigurationsdatei

`config/vectrum.toml` wird beim ersten Start mit den Standardwerten geschrieben. Inhalt und Bedeutung stehen in der README (Abschnitt Configuration), Entscheidungen in E94 bis E96. Wichtig:

- Upgrade-Wirkung ist eine Liste pro Typ, Index = Anzahl eingebauter Upgrades. Länge minus 1 = Maximalzahl (höchstens 63).
- Mengen pro Übertragung für Item, Fluid, Energie, Gas; Intervalle für Wireless und Redstone; Leerlauf, Zielpause, Zeitbudget, Prüfintervall.
- Falsche Einträge werden im Log gemeldet und durch Standardwerte ersetzt.
- Keine Synchronisation zum Client (siehe Punkt 3).

## 3. Konflikte zwischen Performance und Features (zum Besprechen)

1. **Wartezeit leerer Quellen.** Eine Quelle, die lange nichts bewegt hat, wird nur noch alle bis zu 40 Ticks (2 s) geprüft. Ein geplanter Tick lässt sich in Minecraft nicht vorziehen. Ware, die in ein wartendes Lager kommt, wird deshalb bis zu 2 s später bewegt. Einstellbar (`idle_interval_max`, 0 schaltet es aus).
2. **Wireless „unendlich“.** Der Port bewegt weiter alles, was seine Lager hergeben, pro Lauf. Das ist aber nur noch durch die Zeitscheibe und das Zeitbudget begrenzt, nicht durch eine Menge. Ich habe es so gelassen, weil das Zeitbudget die Serverzeit schützt. Frage an dich: Soll Wireless eine endliche, einstellbare Menge pro Lauf bekommen? (E5 sah das als Konfigurationswert vor; ich habe es nicht eingebaut, weil es ein neues Feature wäre.)
3. **Zeitbudget und Zeitscheibe.** Unter hoher Last verzögern sich Übertragungen um Ticks, verloren geht nichts. Ein einzelnes Kabel kann das Budget des Ticks nicht mehr allein aufbrauchen: es darf pro Durchlauf höchstens `run_budget_micros` (2 ms) verbrauchen, der Rest seiner Ware wartet auf den nächsten Durchlauf. Ein Kabel mit extrem hohem Durchsatz bewegt unter Last also weniger pro Sekunde als eingestellt (Durchsatz weicht der Server-Zeit). Fluid, Energie und Gas werden pro Ziel in einem Aufruf bewegt und nicht zerschnitten. Viele Kabel zusammen können das 10-ms-Budget des Ticks weiterhin ausschöpfen, dann rücken später dran kommende Kabel um einen Tick nach.
4. **Zielpause (E31 geändert).** Ein volles Ziel wird bis zu 5 s nicht mehr gefragt, auch wenn es früher Platz hat. Neu: Das gilt jetzt auch, wenn nur dieses eine Ziel voll ist (vorher nur, wenn die Quelle woanders geliefert hat), und eine leere Quelle schickt nichts mehr schlafen.
5. **Prüfintervall der Kabel.** Fähigkeiten eines Nachbarn, die sich ohne Nachbar-Update ändern, bemerkt ein Kabel nach höchstens 100 Ticks.
6. **Erster Start nach dem Setzen.** Der Startversatz verzögert die erste Übertragung um bis zu ein Intervall.
7. **Keine Client-Synchronisation der Konfiguration.** Der Server entscheidet, was fließt. Das Port-Fenster zeigt aber die Maximalwerte der Datei des Clients. Passen Server- und Client-Datei nicht zusammen, stimmen nur diese Anzeigen nicht. Ein Sync wäre eine neue Netzwerknachricht; ich habe ihn nicht gebaut.

## 4. Schwächen und Knackpunkte

Ergebnis: Nichts davon erfordert eine Neudefinition des Mod-Konzepts. Was klein änderbar war, ist erledigt (Abschnitt 1). Offen bleibt nur, was am Konzept „Kabel ohne Block-Entity, Polling über geplante Ticks“ hängt:

| Punkt | Bewertung |
| --- | --- |
| Polling statt Ereignisse | Lager melden Änderungen nicht, das gilt auch für den Konkurrenten. Folge ist die Wartezeit aus Punkt 3.1. Keine Konzeptänderung nötig, nur Abwägung. |
| Eine Datei pro Dimension | `vectrum_networks.dat` wird als Ganzes geschrieben, wenn etwas geändert wurde. Bei sehr großen Servern kann das beim Speichern spürbar werden. Eine Aufteilung (z. B. pro Chunk) wäre ein größerer Umbau; erst sinnvoll, wenn Messungen es zeigen. |
| Verlustfenster auf Forge | Forge hat keine Transaktionen. Geht beim Rücklegen eines Rests etwas schief, wird nur ein Log-Eintrag geschrieben (wie vorher). Auf Fabric sind es Transaktionen. Nicht lösbar ohne Forge-seitige Unterstützung. |
| Große Lager | `canGive` und das Bewegen lesen die Slots; bei sehr vielen Slots kostet das pro Lauf Zeit. Das Leerlauf-Warten und das Zeitbudget begrenzen es, ein Schnellpfad pro Lagermod wäre eine eigene Integration. |
| Wireless über Dimensionen | Beide Chunks müssen geladen sein (Konzept, E60). |

## 5. Stärken des Konkurrenten

| Stärke | Status | Begründung und Auswirkung |
| --- | --- | --- |
| Kanal-Abkühlung mit wachsender Wartezeit (bis 40) | umgesetzt | `idle_interval_max` = 40, gleiche Idee, eigene Umsetzung pro Quellblock. |
| Ablehnungs-Cache, Slot-Urteil-Cache | umgesetzt | Pro Bewegung im Forge-Item-Port, plus Zielpause. |
| Zeitbegrenztes Abarbeiten | umgesetzt | `time_budget_micros` (Standard 10 ms). |
| Staffelung nach Stufen (T0 bis T4) | schon vorhanden | Entspricht den Upgrade-Tabellen, jetzt frei einstellbar. |
| Wecklisten pro Netzwerk | nicht übernommen | Vectrum nutzt Minecrafts geplante Block-Ticks (chunk-sicher, gespeichert, keine Loader-Events). Eigene Wecklisten bräuchten Lade-/Entlade-Logik auf drei Loadern. Gewinn gering, weil die Zielliste ohnehin pro Netzwerk gecacht ist. |
| Optionaler asynchroner Plan/Commit | nicht übernommen | Siehe E104. Beim Konkurrenten ist er standardmäßig aus. |
| BlockCapabilityCache | nicht übernommen | Gibt es nicht für Forge/NeoForge 1.20.1 (erst ab NeoForge 1.20.3); Fabric hat `BlockApiCache`. Gewinn klein gegenüber dem Slot-Lesen (E105). Auf Wunsch für Fabric nachrüstbar. |
| Entity-Knoten, namensbasierte Netze | nicht anwendbar | Widerspricht dem Konzept (Blöcke ohne Block-Entity, Netze durch Berührung und Coder). |

## 6. Dateien, die du löschen musst

- `GROSSAUFTRAG-BERICHT.md` (diese Datei), nachdem du sie gelesen hast.
- Sonst nichts. Die früher markierten Dateien (`*_inner.png`, alte `item_cable_in_*.json`) sind bei dir schon weg.
- Auffällig, aber von mir nicht angefasst: `src/main/resources/assets/vectrum/textures/block/1item_cable.png` und `2item_cable.png` (vermutlich deine Entwürfe).

## 7. Tests

- Unit-Tests (Forge-Projekt): Konfigurationsdatei, Upgrade-Tabellen, Router-Gruppen, abgeleitete Durchsatzgrenzen und Migration alter Welten, Leerlaufzeiten, Startversatz.
- Kompiliert: Fabric, Forge, NeoForge.
- Absturztest (dedizierter Server pro Loader, RCON-Skripte für Sequenziell, Priorität, Round Robin, Balanced, Filter, Digital/Coder, Wireless, Upgrades, Neustart mit Speichern und Laden): Minecraft stürzt auf keinem Loader ab, im Log gibt es keine neuen Fehler. Bekanntes Rauschen wie vorher: `gas_cable` ohne Mekanism (Rezept und Loot-Tabelle) und Netzwerkfehler der Anmeldeserver.
- Der erste Lauf zeigte scheinbar „Ware bleibt in der Quelle“. Ursache war nicht die Laufzeit-Umstellung, sondern die Testskripte: sie stammen aus der Zeit vor E10 und setzen voraus, dass Endpunkte standardmäßig Ausgang sind; seit E10 ist der Standard Eingang. Die Testhilfe setzt jetzt nach jedem `setblock` eines Kabels oder Endpunkts die Rolle Ausgang auf allen Seiten; mit dieser Anpassung stimmen die Ergebnisse mit den früheren überein.
- Einzige verbleibende Abweichung zum früheren Lauf: der Balanced-Test brauchte mehr Wartezeit (1,2 s auf 3 s), weil eine Quelle ohne Ware nach der Gnadenfrist bis zu 2 s wartet (Konflikt 3.1). Das ist das beschriebene Verhalten, kein Fehler.
