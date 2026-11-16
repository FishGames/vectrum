# Ideenliste

Sammelstelle für Ideen, Vorschläge und offene Gedanken. **Nichts hier wird ohne ausdrücklichen Auftrag umgesetzt.**
Einträge dürfen frei sortiert, ergänzt und umgeschrieben werden. Was umgesetzt wird, wandert mit Begründung nach
`decisions.md`; was verworfen wird, wird hier einfach gelöscht.

## Aussehen und Texturen

- Item-Kabel: weitere Teile der Textur (Rückseite der Anschlussplatte, gedrehte Darstellung) Schritt für Schritt nach Vorgabe.
- Die anderen Kabel haben noch Platzhalter-Texturen.

## Netzwerk und Transport

- Filter nach NBT (verzauberte Items usw.), bisher nur nach Registry-Kennung.
- Weiterleiten über mehrere Coder und mehrere Wireless-Ports („Relaying“), bisher bewusst nur ein Sprung.
- Vollständig ereignisgesteuertes Register für volle Ziele statt Back-off mit Wartezeit.
- Eigener Block-Zweck für den Item-Endpoint (und ggf. neuer Name).

## Bedienung und Anzeige

- Netzwerk-Übersicht für den Coder (Name, Farbe, Ort, Status, Durchfluss) und Einfärben der Coder (siehe E3).

## Qualität und Tests

- Clients unter NeoForge und die Integrationen, die bisher nicht in einer echten Umgebung geprüft wurden
  (Jade auf Forge, WTHIT auf Forge/NeoForge, EMI auf Forge, TOP auf NeoForge).
- Verhalten mit echten Fremd-Schraubenschlüsseln (AE2, Create, EnderIO, Mekanism) prüfen.

## Veröffentlichung

- Changelog vor dem ersten Release als Release-Notes in Prosa umschreiben (steht als TODO im Changelog).
