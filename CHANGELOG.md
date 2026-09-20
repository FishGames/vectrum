# Changelog

Alle wichtigen Änderungen an Vectrum. Neueste zuerst.

## Unveröffentlicht

### Hinzugefügt
- Redstone-Kabel (Etappe 8): überträgt Signalstärken (0–15) zwischen Eingängen und Ausgängen eines Netzes. Der Wert
  ist die größte Signalstärke an einem Eingang; jeder Ausgang gibt ihn ab. Neben Hebel, Redstone-Block, Staub usw.
  wird eine Seite von selbst zum Eingang; einen Ausgang stellt man mit dem Schlüssel (oder
  `/vectrum port <pos> <seite> role in|out|off`) ein. Reagiert sofort auf Änderungen, kein Dauer-Polling. Rezept:
  2 Redstone + 1 Eisenbarren ergeben 6 Kabel. Das Diagnosewerkzeug zeigt die Signalstärke.
- Universalkabel (Etappe 7, Stufe 2): ein Kabel, das Items, Fluide und Energie gleichzeitig führt. Jeder Typ bildet
  darin sein eigenes Netz, die Typen vermischen sich nicht. Rezept: je 1 Item-, Fluid- und Energie-Kabel + 1 Goldbarren
  ergeben 2 Universalkabel. Filter wirken nur auf den Typ, für den sie Einträge haben. Das Durchsatzlimit gibt es je
  Typ (`/vectrum throughput <pos> type <item|fluid|energy>`).
- Upgrade-System (Etappe 6): fünf Upgrade-Items (Durchsatz, Tempo, Sorten, Filter, Priorität). Sie werden mit
  Rechtsklick in einen Kabelanschluss gesteckt, mit Schlüssel + Schleichen + Rechtsklick wieder herausgenommen und
  fallen beim Abbauen zurück. Durchsatz vervierfacht das Limit pro Upgrade, Tempo halbiert das Intervall, Sorten
  erlauben eine Item-Sorte mehr pro Übergabe, Filter und Priorität schalten die gleichnamigen Einstellungen frei.
- Befehl `/vectrum upgrade <pos> [add|remove <sorte> [n]|clear]` (nur Operatoren).
- Filter, Priorität und Verteilmodi (Etappe 5): Jede Anschlussseite hat eine Priorität (höhere Zahl wird zuerst
  beliefert), einen Verteilmodus (der Reihe nach, reihum, ausgleichen) und einen Filter (Positiv- oder Negativliste
  für Items und Fluide). Einstellbar mit `/vectrum port <pos> <seite> ...` (nur Operatoren, bis Upgrades und Oberfläche
  folgen); die Einstellungen werden mit der Welt gespeichert. Volle Ziele werden zeitweise seltener gefragt.
- Vanilla-Kessel lassen sich auf Forge und NeoForge mit Fluid-Kabeln befüllen und leeren (voller Eimer), wie auf Fabric.
- Fluide und Energie (Etappe 4): Fluid-Kabel und Energie-Kabel, die wie das Item-Kabel funktionieren (Kabel legen,
  Enden schalten mit dem Schlüssel um). Grundlimits: 1000 mB bzw. 2000 FE pro Übergabe und Quellseite. Auf Fabric wird
  Team Reborn Energy (MIT) mitgeliefert.
- Durchsatzlimit (Etappe 3): Jeder Kabel-Anschluss hat ein gespeichertes Limit (Grundwert 4 Items pro Übergabe und
  Quellseite). Es wird beim Übergeben nur nachgeschlagen, nie über das Netz berechnet, und mit der Welt gespeichert.
- Befehl `/vectrum throughput <pos> [<wert>|reset]` (nur Operatoren) zum Anzeigen und Setzen des Limits.
- Das Diagnosewerkzeug zeigt das Durchsatzlimit des angeklickten Bausteins.
- Rezept-Advancements (Ergebnis des ersten echten Datagen-Laufs).

### Geändert
- Die Rolle einer Kabelseite ist nun „nicht gewählt“, bis der Spieler sie umschaltet (Mengen-Typen: weiterhin Ausgang,
  Redstone: Eingang neben Signalquellen). Bestehende Welten behalten ihre Rollen.
- Fix: `/vectrum throughput <pos> <wert>` funktionierte seit Etappe 7 nicht mehr (nur `... reset <wert>`).
- Ohne Sorten-Upgrade wird pro Übergabe an ein Ziel nur noch eine Item-Sorte bewegt.
- Filter und Priorität wirken nur noch mit dem passenden Upgrade; `/vectrum port` verweigert das Setzen ohne.
- `Port.moveTo` nimmt jetzt einen Filter entgegen und `Port` kennt den Füllstand; `ItemPort` hat keine eigene Filter-Methode mehr.
- Der Transport ist jetzt für alle Typen derselbe Code (`Transport`, `Port`); die alten Item-Klassen entfallen.
- Kabelenden übernehmen die Funktion der bisherigen Endpunkte, ein Item-Kabel reicht für Stufe 1 und 2.
- Grunddurchsatz auf 4 Items alle 0,5 Sekunden gesenkt.
- Das Diagnosewerkzeug ist ein eigenes Item (vorher Teil des Schlüssels).
- Kabel-Blockstate geändert: Welten aus früheren Testständen sind nicht kompatibel.

### Erstes Grundgerüst
- Projekt für Fabric, Forge und NeoForge (Minecraft 1.20.1), Netzwerk-Kern mit Tests, Item-Kabel mit Transport von
  Kiste zu Kiste, Schlüssel, Rezepte, Datagen, Sprachdateien (DE und EN).
