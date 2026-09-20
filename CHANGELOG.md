# Changelog

Alle wichtigen Änderungen an Vectrum. Neueste zuerst.

## Unveröffentlicht

### Hinzugefügt
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
- Der Transport ist jetzt für alle Typen derselbe Code (`Transport`, `Port`); die alten Item-Klassen entfallen.
- Kabelenden übernehmen die Funktion der bisherigen Endpunkte, ein Item-Kabel reicht für Stufe 1 und 2.
- Grunddurchsatz auf 4 Items alle 0,5 Sekunden gesenkt.
- Das Diagnosewerkzeug ist ein eigenes Item (vorher Teil des Schlüssels).
- Kabel-Blockstate geändert: Welten aus früheren Testständen sind nicht kompatibel.

### Erstes Grundgerüst
- Projekt für Fabric, Forge und NeoForge (Minecraft 1.20.1), Netzwerk-Kern mit Tests, Item-Kabel mit Transport von
  Kiste zu Kiste, Schlüssel, Rezepte, Datagen, Sprachdateien (DE und EN).
