# Implementierungs-Prompt

> Dieser Text ist dafür gedacht, an ein KI-Modell oder einen Entwickler übergeben zu werden, um das Konzept umzusetzen. Er beschreibt **nur die Funktion des Mods**. Das Projektgerüst wird als vorhanden vorausgesetzt.
>
> **Noch nicht programmieren — dies ist die Vorbereitung.**

---

## Ausgangslage

Es existiert bereits ein lauffähiges Multiloader-Grundgerüst für **Minecraft 1.20.1** (Forge, Fabric, NeoForge) mit einem Beispielblock, einem Beispielitem, einem Creative Tab und einem Crafting-Rezept. Registrierung, Buildsystem und Datagen-Anbindung sind eingerichtet und funktionieren.

Alles, was folgt, baut darauf auf. Grundgerüst-Themen (Gradle, Mappings, Loader-Setup, Mod-Metadaten) sind **nicht** Teil der Aufgabe.

---

## Aufgabe

Implementiere einen reinen Logistik-Mod, der Items, Fluide, Energie, Redstone-Signale und optional Gase transportiert. Kein Storage, kein Autocrafting, keine Logikverarbeitung.

---

## Nicht verhandelbare Kernentscheidungen

Diese Punkte prägen die gesamte Architektur. Weiche davon nicht ab, ohne nachzufragen.

### K1 — Die Ware reist nicht
Es gibt **keine** Objekte, die sich durch Kabel bewegen. Transport ist ausschließlich eine direkte Übergabe:

```
entnimm X aus Quelle → füge X in Ziel ein
```

Kabelblöcke haben **keine tickende BlockEntity**. Nur Endpunkte handeln, in ihrem eigenen Intervall. Die Rechenlast hängt an der Anzahl der Übergaben, nicht an der transportierten Menge.

### K2 — Zwei streng getrennte Kabelarten

Das zentrale Konzept des Mods. Beide Arten teilen sich Netzwerk-, Routing- und Filterinfrastruktur, unterscheiden sich aber fundamental im Verhalten.

| | **Transportkabel** | **Digitale Kabel** |
|---|---|---|
| Stufen | 1 und 2 | ab 3 |
| Trägt Ware | ja | nein, koppelt nur |
| Durchsatzlimit | **ja** | **nein** |
| Upgrades | **ja** | **nein** |
| Entfernungskosten | nein | nein |

Der Übergang zwischen beiden Welten passiert ausschließlich am **Coder**-Block.

Digitale Kabel dürfen **keine** Upgrade-Slots, keine Limit-Logik und keine Tier-Varianten bekommen. Wenn im Code eine Stelle entsteht, an der ein digitales Kabel etwas deckelt, ist das ein Fehler.

### K3 — Das Durchsatzlimit ist ein gespeicherter Wert, keine Berechnung

Zwingend so umzusetzen:

```
Bei Topologieänderung (selten):  → Limit der Strecke einmal ermitteln, im Netz cachen
Bei jeder Übergabe (oft):        → gecachten Wert nachschlagen, Menge deckeln
```

Das Limit darf **niemals** pro Tick oder pro Übergabe neu über das Kabelnetz berechnet werden. Verhalten nach außen wie Pipez, Rechenaufwand wie ein Nachschlagevorgang.

### K4 — Keine Gerätekontingente

**Begriffsklärung, weil hier zwei Dinge leicht verwechselt werden:**

| Begriff | Bedeutung | Im Mod? |
|---|---|---|
| **Frequenz** | Zuordnungsmerkmal: welcher Coder gehört zu welchem Netz, welcher Sender zu welchem Empfänger, welches Redstone-Signal zu welcher Leitung | **ja**, unbegrenzt viele |
| **Gerätekontingent** (AE2-„Channel") | eine Leitung führt nur N angeschlossene Geräte, darüber hinaus fallen Geräte aus | **nein** |

Verboten ist ausschließlich das Zweite. Es gibt keine Obergrenze für die Anzahl angeschlossener Endpunkte, Coder oder Sender in einem Netz, und kein Gerät fällt jemals aus, weil ein Kontingent erschöpft ist.

Verwende im Code und in allen Texten durchgängig **Frequenz**, nicht „Kanal" — auch nicht bei Redstone —, damit die Verwechslung gar nicht erst entsteht.

Ebenfalls nicht vorgesehen: Reichweitengrenzen und Entfernungskosten. Das Balancing zwischen den Stufen läuft über Rezeptkosten, innerhalb einer Stufe über Upgrades.

### K5 — Intern `long`, an der Schnittstelle auf `int` kappen

Rechne **intern** mit `long`. An der Grenze zu fremden Schnittstellen wird sauber auf `int` geklemmt.

**Warum nicht einfach überall `int`:** Ein `int` reicht bis rund 2,1 Milliarden und deckt jede realistische Transfermenge problemlos ab. `long` ist hier **nicht** nötig, um große Mengen zu bewegen, sondern um stille Überläufe zu vermeiden:

- Zwischenrechnungen bei Upgrades (`Basis × Faktor` kann überlaufen, auch wenn das Ergebnis passt)
- Summen über mehrere Ziele oder mehrere Coder
- Statistik- und Diagnosezähler über längere Zeiträume

**Die tatsächliche Grenze liegt bei den fremden Schnittstellen, nicht bei uns.** In 1.20.1 arbeiten die relevanten APIs mit `int`: Forge Energy nimmt `int` pro Aufruf, `FluidStack` führt die Menge als `int`, `ItemStack` ohnehin. Eine Übergabe kann deshalb nie mehr als `Integer.MAX_VALUE` in einem Aufruf bewegen, egal wie groß der interne Wert ist.

**Konkret umzusetzen:**
- Eine zentrale Hilfsfunktion zum Klemmen (`long → int`, gesättigt, nie überlaufend). Alle Aufrufe fremder Handler gehen darüber.
- Kein eigenes Zahlenkonzept im Stil von Mekanisms Energiesystem. Der Aufwand lohnt sich für diesen Mod nicht.
- Optionale Anbindung an `long`-basierte Energiesysteme (Mekanism) gehört ins Kompatibilitätsmodul, nicht in den Kern.
- Der praktische Flaschenhals ist ohnehin die Kapazität des Zielinventars.

### K6 — Viele unabhängige Netze
Jede zusammenhängende Kabelstrecke ist ein eigenes Netz. Es gibt kein globales Netz. Verbinden verschmilzt Netze, Trennen spaltet sie. Digitale Netze koppeln mehrere Transportnetze zu einem gemeinsamen Vermittlungsraum, ohne sie zu verschmelzen.

### K7 — Funktioniert ohne Konfiguration
Quellendpunkt setzen, Zielendpunkt setzen — es läuft. Standardwerte: Filter leer = alles, Priorität neutral, Modus `SEQUENTIAL`.

---

## Architektur

### Modulaufbau

```
common/
  network/     Graph, Merge/Split, Registry aller Netze
  routing/     Angebots-/Nachfrage-Register, Matching, Modi
  transport/   Typ-Abstraktion (Item, Fluid, Energie, Redstone, Gas)
  throughput/  Limit-Ermittlung und Caching (nur Transportkabel)
  filter/      Filter-Engine
  upgrade/     Upgrade-Definitionen und -Verwaltung
  api/         öffentliche, versionierte Schnittstelle für andere Mods
platform/      loaderspezifische Capability-/Registry-Anbindung
client/        Rendering, GUIs
data/          Datagen
```

**Anforderung:** `common/network`, `common/routing`, `common/throughput` und `common/filter` müssen **ohne laufendes Minecraft** testbar sein. Keine Minecraft-Imports in diesen Paketen. Positionen über eine eigene leichtgewichtige Koordinaten-Abstraktion.

### Netzwerkmodell

- Graph aller verbundenen Kabelpositionen, im Speicher gehalten.
- **Inkrementelles** Merge beim Setzen und Split beim Abbauen. Kein vollständiger Neuaufbau bei jeder Blockänderung.
- Kabel tragen **keine** BlockEntity, wenn irgend vermeidbar. Verbindungszustand in den BlockState, Netzzugehörigkeit in eine zentrale Registry.
- Persistenz über eine zentrale, weltgebundene Datenstruktur — nicht verteilt über tausende BlockEntity-NBTs.
- Ein Netz ohne aktive Endpunkte darf **null** Rechenzeit verbrauchen.
- Beim Netzaufbau wird für Transportnetze zusätzlich der Durchsatzwert ermittelt und gecacht (siehe K3).

### Angebots-/Nachfrage-Register

Kern des Routings. Pro Netz und pro Transporttyp:

- Quellendpunkte registrieren, was sie anbieten.
- Zielendpunkte registrieren, was sie akzeptieren (Filter, Priorität).
- Das Register wird **ereignisgesteuert** aktualisiert — bei Konfigurationsänderung, Nachbarschaftsänderung, Voll-/Leermeldung. **Nicht** jeden Tick neu aufgebaut.
- Volle Ziele deregistrieren sich selbst und melden sich bei wieder freiem Platz zurück. Ein volles Ziel blockiert niemals das restliche Netz.
- Quelle und Ziel kennen einander nie direkt.
- Ein digitales Netz führt das Register über alle angeschlossenen Transportnetze hinweg.

### Routing-Kaskade

Bei jeder Übergabe, in dieser Reihenfolge:

1. **Filter** — Menge der in Frage kommenden Ziele bilden
2. **Priorität** — absteigend sortieren
3. **Modus** — bei Gleichstand entscheiden:
   - `SEQUENTIAL` (Standard): erstes Ziel bis voll, dann nächstes
   - `ROUND_ROBIN`: reihum je Übergabe, Zeiger pro Quelle persistent
   - `BALANCED`: Menge so verteilen, dass Füllstände angeglichen werden

Der Modus wird am **Quellendpunkt** konfiguriert. Jede Quelle führt ihren eigenen Round-Robin-Zeiger; mehrere Quellen im selben Netz beeinflussen sich nicht.

### Performance-Vorgaben

- Capabilities/Handler von Nachbarblöcken **cachen**, mit Invalidierung bei Nachbarschaftsänderung. Kein Nachschlagen pro Tick.
- Kapazitätsabfragen am Ziel dürfen nicht jeden Slot einzeln durchzählen. Ergebnisse zwischenspeichern.
- Endpunkte in nicht geladenen Chunks sind inaktiv, ohne Fehler und ohne Netzumbau.
- Richtwerte: 10.000 Kabelblöcke unter 0,5 ms/Tick; 500 aktive Endpunkte unter 2 ms/Tick; Netz-Neuaufbau bei 10.000 Knoten unter 50 ms und nicht blockierend.

---

## Transporttypen

| Typ | Umsetzung |
|---|---|
| Items | Vanilla-Inventare und Item-Handler-Schnittstellen |
| Fluide | Fluid-Handler-Schnittstellen |
| Energie | Forge Energy / loaderäquivalent |
| Redstone | eigenes Signal-Netz, Übertragung Punkt zu Punkt, optional über Frequenzen |
| Gase | **weiche** Abhängigkeit auf Mekanism |

**Gas-Modul:** Vollständig optional. Fehlt Mekanism, existiert der Typ nicht — keine Registrierung, kein Item, kein Crash, keine Log-Fehler. Isoliertes Kompatibilitätsmodul, darf nicht in den Kerncode einsickern.

**Redstone:** ausschließlich Übertragung. Keine Bedingungen, keine Verknüpfungen, keine Arithmetik.

Alle Typen laufen über **dieselbe** Infrastruktur, in beiden Kabelarten. Entsteht für einen Typ Sonderfallcode im Kern, ist die Abstraktion falsch geschnitten.

---

## Die vier Ausbaustufen

### Stufe 1 — Einzelkabel *(Transportkabel)*
Getrennte Kabelblöcke pro Typ (Item, Fluid, Energie, Redstone, optional Gas). Jeder führt ausschließlich seinen Typ. Kein Modus, kein Typ-Selektor.

Dazu Endpunktblöcke an Inventaren und Maschinen. Ein Endpunkt ist Quelle oder Ziel; die Rolle wird beim Platzieren oder per Wrench gesetzt.

Durchsatzlimit aktiv, aufrüstbar.

### Stufe 2 — Universalkabel *(Transportkabel)*
Ein einzelner Block, der alle Typen gleichzeitig führt. Das Rezept verbraucht Stufe-1-Kabel.

Intern bildet jeder Typ sein **eigenes, getrenntes Netz** an derselben Blockposition. Keine Vermischung. Keine Konfiguration am Kabel — was fließt, entscheiden die Endpunkte.

Durchsatzlimit aktiv, gleiche Upgrades wie Stufe 1.

### Stufe 3 — Digitales Netz *(digitale Kabel + Coder)*

Zwei neue Blöcke:

- **Digitales Kabel** — verbindet Coder untereinander. Kein Limit, keine Upgrades, keine Konfiguration, keine Tier-Varianten.
- **Coder** — Übergang zwischen Transportnetz und digitalem Netz. Ein Block; Rolle ergibt sich aus der Flussrichtung.

```
Quelle → Transportkabel → Coder ⟶ digitales Kabel ⟶ Coder → Transportkabel → Ziel
```

- Stern-/Baum-Topologie. Beliebig viele Coder pro digitalem Netz.
- Ein Coder überträgt **alle** Typen des angeschlossenen Transportnetzes.
- Entfernung irrelevant, kein Durchsatzdeckel auf der digitalen Strecke.
- Die Transportnetze an beiden Enden funktionieren unverändert weiter, inklusive Limit und Upgrades.
- Kein zentraler Manager, keine Programmiersprache, keine Speicherung, kein Terminal.

### Stufe 4 — Kabellos
Ein Block pro Gerät, direkt am Inventar. Keine Kabel.

- Kopplung über Frequenz. Mehrere Blöcke pro Frequenz erlaubt; ein Sender verteilt auf beliebig viele Empfänger.
- Pro Block und pro Typ einstellbar: senden, empfangen oder beides.
- Teures Rezept, **keine** laufenden Energiekosten.
- **Offen:** Durchsatzlimit wie ein Transportkabel-Endpunkt oder unbegrenzt wie ein digitales Netz. Vor der Umsetzung klären; bis dahin so bauen, dass beides über einen Konfigurationswert abbildbar ist.

---

## Upgrades

Ein **einziger** Upgrade-Satz, gültig für alle **Transportkabel**-Stufen. Frei umsteckbar.

| Upgrade | Wirkung |
|---|---|
| Durchsatz | höheres Limit pro Übergabe — sehr hoch skalierbar, `long` |
| Geschwindigkeit | kürzeres Intervall zwischen Übergaben |
| Typenanzahl | mehr gleichzeitige Item-Typen |
| Filter | schaltet Filterslots frei |
| Priorität | schaltet Prioritätseinstellung frei |

Upgrades werden **im Block** gehalten, nicht im Item. Ein abgebauter Block gibt Block und Upgrades vollständig zurück.

**Digitale Kabel nehmen keine Upgrades an.** Sie haben keine Slots und keine aufrüstbaren Werte.

Es gibt **keine** gestaffelten Kabelvarianten (Basic/Advanced/Elite). Unterschiede in Zahlen laufen über Upgrades, Unterschiede im Konzept über eine neue Stufe.

---

## Handhabung und Bedienung

Funktionale Anforderungen, keine Kosmetik:

- **Kabelmaße** (angelehnt an AE2 Dense Cables), bei 16px Blockbreite:

  | Aspekt | Methode | Breite |
  |---|---|---|
  | Kollision (Anstoßen des Spielers) | `getCollisionShape` | **6 px** |
  | Auswahl und Platzierung (Zielen mit dem Fadenkreuz) | `getShape` | **10 px** |
  | Modell / Darstellung | — | **10 px** |

  Die Auswahlbox ist also bewusst breiter als die Kollisionsbox. Der Spieler kann optisch leicht in ein Kabel hineinlaufen, trifft es aber beim Rechtsklick zuverlässig. Das ist beabsichtigt.

  Verbindungsstücke zu Nachbarblöcken und Endpunkten folgen der 10px-Darstellung.

- **Kein Item des Mods ist unstackbar.** Alle Einstellungen leben im Block.
- **Eine Funktion braucht höchstens einen Block** — Definition siehe unten.
- **Wrench-Werkzeug:** Rolle umschalten, Verbindungen an-/abschalten, Konfiguration kopieren (Shift-Rechtsklick überträgt Einstellungen eines Endpunkts auf einen anderen).
- **Abbauen gibt immer alles zurück**, in einem Zug.
- **Diagnose:** Ein Endpunkt muss sichtbar machen, warum nichts fließt. Mindestens: „kein Ziel", „Ziel voll", „Filter blockiert alles", „Ziel in nicht geladenem Chunk", „Durchsatzlimit erreicht". Ausgabe über Tooltip-Mods (Jade/WTHIT/TOP) und in der GUI.

### Definition: „Eine Funktion braucht höchstens einen Block"

**Eine Funktion** ist ein Verbindungspunkt, der genau eine Aufgabe erfüllt — zum Beispiel „entnimm Items aus dieser Kiste und gib sie ins Netz" oder „lege Fluide in diesen Tank".

**Die Regel:** Um eine Funktion herzustellen, darf der Spieler **niemals** mehrere eigenständige Bauteile kombinieren müssen. Ein Block platzieren, und die Funktion existiert.

**Erlaubt**, weil es die Regel nicht verletzt:

| Fall | Warum in Ordnung |
|---|---|
| Upgrades im Endpunkt | Sie sind kein eigenständiges Bauteil, sondern Zubehör. Ohne sie funktioniert der Endpunkt bereits. |
| Endpunkt + Kabel | Das Kabel ist Infrastruktur, keine Funktion. Es wird einmal gelegt und bedient viele Endpunkte. |
| Coderpaar (Stufe 3) | Zwei Blöcke, aber an zwei Orten — je ein Verbindungspunkt. Nicht zwei Bauteile für einen Punkt. |
| Sender/Empfänger (Stufe 4) | Dasselbe Argument. |

**Nicht erlaubt**, das sind die Muster, die die Regel verhindern soll:

- Ein separater Filterblock, den man neben den Endpunkt setzen muss
- Ein Controller, ohne den Grundfunktionen nicht laufen
- Trägerblock plus eingesetztes Modul plus Konfigurationskarte für eine einzelne Verbindung (Integrated-Dynamics-Muster)
- Ein Adapter zwischen Endpunkt und Kabel
- Getrennte Blöcke für Eingang und Ausgang **am selben Ort** — die Rolle ist eine Einstellung, kein anderer Block

**Prüffrage bei jedem neuen Block:** Braucht der Spieler diesen Block *zusätzlich* zu einem anderen, um an *einer* Stelle *eine* Sache zu tun? Wenn ja, gehört die Funktionalität in den vorhandenen Block.

### GUI

- Ein Endpunkt-GUI, für alle Typen gleich aufgebaut.
- Filter per Drag-and-drop aus JEI/EMI (Ghost-Items).
- Live-Anzeige des tatsächlichen Durchsatzes und des aktuellen Limits.
- Optionen, die nicht freigeschaltet sind, werden **nicht angezeigt** — nicht ausgegraut. Ein Spieler ohne Filter-Upgrade sieht keine Filterslots.
- Ein digitales Kabel hat **keine** GUI.

---

## Kompatibilität

**Erforderlich:** Vanilla-Inventare, Item-/Fluid-/Energie-Handler, JEI **und** EMI, Jade/WTHIT/TOP.

**Erwünscht:** Mekanism (Gase), AE2 (anschließbar, nicht ersetzend), Refined Storage, Create, Immersive Engineering, Sophisticated Storage.

**Öffentliche API:** Sauber versioniert, damit Fremdmods eigene Endpunkt- und Transporttypen beisteuern können.

---

## Ausdrücklich nicht implementieren

- Storage-Netzwerk, Terminal, Suchfunktion über Inhalte
- Autocrafting jeglicher Art
- Gerätekontingente im Stil der AE2-Channels
- Limits oder Upgrades auf digitalen Kabeln
- Redstone-Logik, Bedingungen, Rechenoperationen
- Maschinen, Erze, Energieerzeugung, Dimensionen
- Sichtbar wandernde Ware im Kabel (frühestens nach v1.0, rein clientseitig)

---

## Reihenfolge der Umsetzung

1. Netzwerkkern: Graph, Merge, Split — mit Unit-Tests, ohne Minecraft
2. Item-Transportkabel und Endpunkte, nur Items, ohne Filter — Ende-zu-Ende lauffähig
3. Durchsatzlimit mit Caching nach K3
4. Fluide und Energie über dieselbe Infrastruktur
5. Angebots-/Nachfrage-Register, Filter, Priorität, Modi
6. Upgrade-System
7. Stufe 2
8. Redstone
9. Stufe 3: digitales Kabel und Coder
10. Stufe 4
11. Gas-Modul (optional, isoliert)
12. Diagnose, Tooltip-Anbindung, GUI-Feinschliff
13. Rezepte und Datagen

Schritt 2 ist der wichtigste Meilenstein: sobald ein Item von Kiste A nach Kiste B wandert, steht die Architektur oder sie steht nicht.

---

## Fertigstellungskriterium pro Feature

Code, Tests, Datagen, Sprachdateien (DE und EN), Changelog-Eintrag.
