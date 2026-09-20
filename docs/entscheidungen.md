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
| E2 | **Anschlüsse:** Ein Netzknoten, der an ein Inventar grenzt, ist ein Anschluss (Quelle oder Ziel). Das gilt für Kabelenden ebenso wie für den Endpunkt-Block (siehe E16, ersetzt die frühere Fassung von E2). Rolle, Filter, Priorität und Modus gelten pro angeschlossener Seite, Upgrades pro Anschlussblock. | Bedienung bleibt ein GUI mit Seitenauswahl. |
| E3 | **Übersicht im digitalen Netz:** (1) Coder haben einen frei wählbaren Namen. (2) Coder lassen sich mit Farbstoff einfärben. (3) Das Wrench zeigt per Klick auf einen Coder oder ein digitales Kabel eine Netzübersicht: Liste aller Coder mit Name, Farbe, Ort, Status und aktuellem Fluss. Es zeigt keine Inhalte, damit es kein Terminal wird. (4) Mit dem Wrench in der Hand wird das ganze Netz umrissen. | Ein einziges Werkzeug statt eines neuen Blocks; hält die Regel "eine Funktion, höchstens ein Block" ein und verletzt das Verbot von Storage/Terminal nicht. Umsetzung in Etappe 9 und 12. |
| E4 | **Stufe 4 bekommt Upgrade-Plätze**, mindestens für ein neues **Dimensions-Upgrade**. Konzept-Änderung: Die Upgrade-Tabelle wird um "Dimensionen" ergänzt. Digitale Kabel bleiben ohne Upgrades. | Folgt aus P6. |
| E5 | **Stufe-4-Durchsatz:** standardmäßig unbegrenzt, über einen Konfigurationswert umschaltbar (wie im Prompt gefordert). | Ein Limit nachträglich einzuführen ist einfacher, als es wieder zu entfernen. |
| E6 | **Hauptumgebung Forge.** Forge und NeoForge teilen sich in 1.20.1 einen Anschluss (`platform/forge`), Fabric hat einen eigenen. Für Energie auf Fabric wird Team Reborn Energy eingebaut (Jar-in-Jar), die Lizenz ist vorher zu prüfen. | Die Wunschmods (Mekanism, Create, Refined Storage, ...) laufen unter 1.20.1 überwiegend auf Forge. |
| E7 | **Kern ohne Minecraft:** Alles unter `io.github.fishgames.vectrum.core` importiert nichts aus Minecraft, Forge, Fabric oder Mojang-Bibliotheken. Ein Test prüft das automatisch. | Der Kern soll ohne laufendes Minecraft testbar sein. |
| E8 | **Koordinaten enthalten die Dimension** (`BlockCoord`). Das drahtlose Frequenzregister (Stufe 4) wird später serverweit geführt, alle anderen Netze pro Dimension. | Hält P6 offen, ohne den Kern später umbauen zu müssen. |
| E9 | **Redstone** wird als Transporttyp mit anderem Verhalten (`SIGNAL`: Wert spiegeln) angelegt, alle anderen Typen als `QUANTITY` (Menge übergeben). | Vermeidet Sonderfallcode im Kern, sobald Redstone dazukommt. |

## Etappe 2 (Item-Kabel und Endpunkte), von Claude getroffen

| Nr. | Entscheidung | Begründung |
| --- | --- | --- |
| E10 | **Standard-Rolle einer Anschlussseite ist Ausgang (Ziel).** Das Wrench schaltet die Rolle weiter: Ausgang, Eingang, Aus, wieder Ausgang. Gemeint ist die Seite mit angeschlossenem Inventar, die der Klickstelle am nächsten liegt (die Fläche direkt an der Kiste lässt sich ja nicht anklicken). | Ein neues Kabelende zieht so nie ungewollt Ware aus einer Kiste. |
| E11 | **Netzzugehörigkeit und Rollen der Anschlussseiten werden mit der Welt gespeichert** (`data/vectrum_networks.dat` je Dimension) und beim Laden über den Kern neu aufgebaut. Kabel und Endpunkte haben keinen Blockentity. | Kabel können sich beim Laden eines Chunks nicht selbst melden. Der Neuaufbau kostet nach Messung rund 10 ms für 10.000 Bausteine. Auch spätere Upgrades und Filter gehören in diese dünn besetzte Ablage statt in einen Blockentity pro Kabel. |
| E12 | **Verbindungen stehen im BlockState.** Kabel: 6 Ja/Nein-Werte. Endpunkt: pro Seite kein/Kabel/Eingang/Ausgang. Aktualisiert wird in `neighborChanged`; Nachbarn in nicht geladenen Chunks werden nie angefasst (sonst würden Chunks geladen oder Verbindungen am Chunkrand verloren gehen). | Modell, Form und Netz lesen denselben Zustand. |
| E13 | **Transport in Etappe 2:** alle 10 Ticks gibt jede Quellseite bis zu **4** Items an die Ziele im Netz ab, der Reihe nach nach Position. Der Takt läuft über geplante Block-Ticks von Minecraft (kein Blockentity, keine Loader-Ereignisse). Es gibt noch keinen Filter, keine Priorität und keine Modi (Etappe 5) und noch keine Upgrades (Etappe 6). Die Zielliste je Netz wird zwischengespeichert und nur bei Änderungen neu berechnet. | Kleinster Ablauf, der den Kern von Anfang bis Ende belegt. Durchsatz 4 auf Wunsch des Projektinhabers. |
| E14 | **Zugriff auf Inventare über eine eigene Schnittstelle** (`ItemPort`): Forge/NeoForge über die Item-Handler-Capability, Fabric über die Transfer API der Fabric API. Der gemeinsame Code kennt nur `moveTo`. | Der gemeinsame Code bleibt frei von Loader-Klassen. Fluide und Energie folgen später nach demselben Muster. |
| E15 | Kabel und Endpunkte sind noch nicht wasserlogged. Beide Modelle sind Platzhalter aus Quadern, Texturen sind Platzhalter. | Kommt mit den Modellen und Texturen des Projektinhabers. |

| E16 | **Kabelenden übernehmen die Endpunkt-Funktion (Stufe 1 und 2).** Ein Kabel, das an ein Inventar grenzt, wird von selbst zum Anschluss. Im Kern wird es dann vom Kabel zum Endpunkt (`setKind`), ein Netz ohne solche Knoten kostet weiterhin nichts. Kabel und Endpunkt teilen sich die Basisklasse `ConduitBlock`. Der Wunsch kam vom Projektinhaber nach dem ersten Test. | Grundbetrieb ohne Zusatzblock: Kabel legen, fertig. |
| E17 | **Der Endpunkt-Block bleibt vorerst erhalten** (immer aktiver, dickerer Knoten) und bekommt später eine andere Aufgabe und einen anderen Namen. | Wunsch des Projektinhabers; entscheiden, sobald die Aufgabe feststeht. |
| E18 | **Diagnosewerkzeug als eigenes Item** (Rechtsklick auf Kabel oder Endpunkt: Netznummer, Größe, Anzahl Eingänge und Ausgänge). Das Wrench ist nur noch zum Einstellen da. | Wunsch des Projektinhabers; hier wächst später die ausführliche Diagnose (Etappe 12). |

## Etappe 3 (Durchsatzlimit), von Claude getroffen

| Nr. | Entscheidung | Begründung |
| --- | --- | --- |
| E19 | **Durchsatzlimit = gespeicherte Zahl pro Anschlussbaustein** (K3, umgesetzt nach E1). Die Tabelle `ThroughputLimits` im Kern hält nur Bausteine mit eigenem Wert; alle anderen haben das Grundlimit (4 Items pro Übergabe und Quellseite). Beim Übergeben genügt ein einziger Nachschlag, nie eine Berechnung über das Netz. Gilt für die Quelle (dort wird entnommen); Ziele bremst nur ihre Kapazität. Ein Wert von 0 hält den Baustein an. Die Werte werden mit der Welt gespeichert (Ebene `limits` in `vectrum_networks.dat`) und beim Abbauen vergessen. | Kabel haben keine Daten (E1), also kann das Limit nur am Anschluss hängen. Die Durchsatz-Upgrades (Etappe 6) tragen denselben Wert ein, dafür ist nichts mehr umzubauen. |
| E20 | **Verwaltungsbefehl** `/vectrum throughput <pos> [<wert>\|reset]` (nur Operatoren): zeigt, setzt oder löscht das Limit eines Bausteins. Das Ergebnis des Zeigen-Befehls ist der Wert (für `/execute store`). Das Diagnosewerkzeug nennt das Limit des angeklickten Bausteins. | Ohne Upgrades ist das der einzige Weg, das Limit im Spiel zu sehen und auszuprobieren. Bleibt als Verwaltungswerkzeug erhalten. |

## Etappe 4 (Fluide und Energie), von Claude getroffen

| Nr. | Entscheidung | Begründung |
| --- | --- | --- |
| E21 | **Ein gemeinsamer Transport für alle Mengen-Typen.** Der Zugang zu Speichern läuft über `Port` (`moveTo(Ziel, Menge)`), je Transporttyp und Loader gibt es eine Umsetzung: Items (Item-Handler bzw. Transfer API), Fluide (Fluid-Handler bzw. Transfer API), Energie (Forge Energy bzw. Team Reborn Energy). `Transport.run` kennt nur noch den Typ des Blocks. Einheiten: Items in Stück, Fluide in mB, Energie in FE (1 E der Team Reborn API = 1 FE). Fabric zählt Fluide in Tropfen, das wird an der Grenze umgerechnet (1 mB = 81 Tropfen). | Genau ein Weg für alle Typen; Redstone und Gas kommen später als weitere Ports bzw. Sonderfall. |
| E22 | **Getrennte Kabelblöcke pro Typ (Stufe 1):** Item-Kabel, Fluid-Kabel, Energie-Kabel. Kabel verschiedener Typen verbinden sich nicht. Ein Kabel erkennt nur Speicher seines eigenen Typs als Anschluss (ein Fluid-Kabel neben einer Kiste tut nichts). | So verlangt es der Prompt (Stufe 1); Stufe 2 fasst sie später zu einem Block zusammen. |
| E23 | **Grundlimits pro Übergabe und Quellseite:** Items 4, Fluide 1000 mB (ein Eimer), Energie 2000 FE (200 FE pro Tick). Alle Typen takten alle 10 Ticks. Die Werte stehen in `TransportDefaults` und sind leicht änderbar. | Erste Schätzung für ein ausgewogenes Grundspiel; die Durchsatz-Upgrades (Etappe 6) heben sie an. |
| E24 | **Energie auf Fabric über Team Reborn Energy 3.0.0** (MIT-Lizenz, geprüft), per Jar-in-Jar mitgeliefert. Fabric hat keine eigene Energie-API. Damit ist der offene Punkt aus E6 erledigt. | Nutzer brauchen dafür keine zusätzliche Mod. |
| E25 | Kein Fluid- oder Energie-Endpunkt-Block. Kabelenden übernehmen den Anschluss (E16). | Der Endpunkt-Block bekommt später eine andere Aufgabe (E17). |

| E26 | **Vanilla-Kessel als Fluidtank auch auf Forge und NeoForge.** Forge kennt für Kessel keine Capability, deshalb gibt es einen eigenen Handler (`CauldronFluidHandler`): leer, voller Wasserkessel oder voller Lavakessel, nur in ganzen Eimern (1000 mB). Teilweise gefüllte Wasserkessel (Stufe 1 und 2) sind auf Forge nicht nutzbar, auf Fabric geht dagegen jede Stufe (dort liefert die Transfer API Drittel-Eimer). | Gleiches Verhalten auf allen Loadern, und Fluide lassen sich ohne Fremdmod ausprobieren. |

## Etappe 5 (Filter, Priorität, Verteilmodi), von Claude getroffen

| Nr. | Entscheidung | Begründung |
| --- | --- | --- |
| E27 | **Routing-Kaskade Filter → Priorität → Modus** (K6/K7), umgesetzt im Kern (`core/routing`, ohne Minecraft, mit Tests). Der Filter wirkt beim Übergeben (er prüft jede konkrete Ware), die Priorität sortiert die Ziele in Gruppen (höhere Zahl zuerst, Standard 0, auch negativ), der Modus verteilt innerhalb einer Gruppe. Nichts geht verloren: es wird nur bewegt, was das Ziel annimmt. | Reihenfolge wie im Prompt. Ohne Minecraft testbar, auch mit Zufallstests (Summe bewegt = min(Budget, freier Platz) in allen Modi). |
| E28 | **Einstellungen hängen an der Anschlussseite** (Position des Kabels/Endpunkts + Seite), nicht am Kabelnetz. `PortSettings` = Priorität, Verteilmodus, Filter. Nur Abweichungen vom Standard werden gespeichert (`PortSettingsTable`, Ebene `settings` in `vectrum_networks.dat`) und beim Abbauen vergessen. Priorität und Filter gelten, wenn die Seite **Ziel** ist; der Verteilmodus und der Filter gelten, wenn sie **Quelle** ist. Beide Filter (Quelle und Ziel) müssen eine Ware durchlassen. | Kabel haben keine Daten (E1); wie beim Durchsatz (E19) genügt eine dünn besetzte Tabelle. |
| E29 | **Filter = Positiv- oder Negativliste über Kennungen** (`minecraft:cobblestone`, `minecraft:water`). Eine leere Liste lässt alles durch, egal welche Art (K7). Energie kennt keine Sorten und ignoriert den Filter. Die Kennung kommt aus der Registry, nicht aus dem Namen. NBT-Unterscheidung (verzauberte Items usw.) gibt es noch nicht. | Einfach, sprachunabhängig und für Items und Fluide gleich. NBT-Filter können später als Erweiterung folgen. |
| E30 | **Verteilmodi:** *der Reihe nach* (Standard: erstes Ziel füllen, dann das nächste), *reihum* (jede Übergabe beginnt beim Ziel hinter dem zuletzt belieferten; der Zeiger wird je Quellseite gespeichert) und *ausgleichen* (Gewicht = 1 − Füllstand, mindestens 0,05; was ein Ziel nicht nimmt, geht in der nächsten Runde an die übrigen). Der Füllstand wird nur im Modus *ausgleichen* abgefragt (`Port.fillLevel`). „Reihum“ rotiert je Übergabe, nicht je Item. | Deckt die üblichen Wünsche ab und bleibt billig. |
| E31 | **Wartezeit für volle Ziele (Näherung des Ereignis-Registers).** Nimmt ein Ziel trotz Angebot nichts an, während die Quelle in derselben Übergabe an andere geliefert hat, wird es nach 20, 40, 80, dann 100 Ticks (5 s) wieder gefragt. Jede Änderung an Netz oder Einstellungen hebt alle Wartezeiten auf. Wartezeiten werden nicht gespeichert. Hat die Quelle gar nichts geliefert, gibt es keine Wartezeit (sonst würden leere Quellen alle Ziele schlafen legen). | Spart Nachfragen bei vielen vollen Zielen. Ein vollständig ereignisgesteuertes Register (Speicher meldet „frei“) folgt bei Bedarf; der Preis ist bis zu 5 s Verzögerung, wenn ein volles Ziel wieder Platz hat. |
| E32 | **Befehl** `/vectrum port <pos> <seite> [priority <zahl> \| mode <sequential\|round_robin\|balanced> \| filter add\|remove <id> \| filter clear \| filter type whitelist\|blacklist \| reset]` (nur Operatoren). Ohne Zusatz zeigt er die Einstellungen (Ergebnis = Priorität). Ungeprüfte Kennungen werden beim Hinzufügen abgelehnt (weder Item noch Fluid). Die Einstellungen sind noch nicht durch Upgrades freigeschaltet; das kommt in Etappe 6, die Oberfläche in Etappe 12. | Ohne Oberfläche der einzige Weg, die Funktionen im Spiel zu nutzen und zu testen. |

## Noch offen

- Verfügbarkeit von JEI, EMI und Jade/WTHIT/TOP für alle drei Loader in 1.20.1 prüfen.
- Ob Mekanism für NeoForge 1.20.1 existiert (Gas-Modul), vor Etappe 11 klären.
