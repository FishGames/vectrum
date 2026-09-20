# Logistik-Mod — Konzeptpapier

> **Status:** Entwurf zur Diskussion. Noch kein Code, keine Namen, keine Rezepte final.
> **Zweck dieses Dokuments:** Das Konzept anderen Spielern vorstellen und Feedback einholen.

---

## 1. Was ist das?

Ein reiner Logistik-Mod, der **Items, Fluide, Energie, Gase und Redstone-Signale über einen gemeinsamen Baukasten transportiert**. Man lernt das Konzept einmal und kann danach alles. Die Bedienung wächst in Stufen mit dem Spieler mit — vom ersten Kabel bis zur durchoptimierten Fabrik.

### Was der Mod bewusst NICHT ist

| Abgrenzung | Begründung |
|---|---|
| **Kein Storage-System** | Wir transportieren. Speichern machen andere. |
| **Kein Autocrafting** | Wir versorgen die Maschinen, die craften. |
| **Keine Logikverarbeitung** | Wir übertragen Redstone-Signale, wir verarbeiten sie nicht. |
| **Kein Ersatz für AE2** | Wir laufen friedlich daneben, in demselben Modpack. |

Der letzte Punkt ist ausdrücklich gewollt. AE2 ist seit vielen Jahren der Maßstab für Storage und Autocrafting, und die Community hat sich darauf eingestellt. Dieser Mod tritt nicht dagegen an. Er bietet eine Alternative für den Bereich **Transport** — und zwar eine, die für alle Spielertypen gleichzeitig funktioniert.

---

## 2. Zielgruppen

Die Hauptzielgruppe sind **Casual-Spieler**, weil sie den Großteil der Kitchen-Sink-Modpacks spielen. Gleichzeitig soll niemand ausgeschlossen werden.

| Gruppe | Bisher bedient durch | Was sie hier bekommt |
|---|---|---|
| **Casual** | Pipez | Stufe 1: hinbauen, läuft. Keine Konfiguration nötig. |
| **Enthusiast** | AE2, Mekanism, EnderIO | Stufe 2–3: Filter, Prioritäten, Verteilmodi |
| **Min-Maxer** | Integrated Dynamics, LaserIO, SFM | Stufe 3–4: Topologie, keine Limits mehr, kabellos |

Heute muss ein Spieler für jeden dieser Bereiche einen anderen Mod lernen. Hier ist es ein System, das mitwächst.

---

## 3. Die zwei Kabelarten

Das ist das zentrale Konzept des Mods. Es gibt **zwei grundverschiedene Sorten von Kabeln**, und sie werden nie miteinander verwechselt.

| | **Transportkabel** | **Digitale Kabel** |
|---|---|---|
| Vorkommen | Stufe 1 und 2 | ab Stufe 3 |
| Was fließt darin | die Ware selbst | nichts — sie koppeln nur |
| Durchsatzlimit | **ja**, begrenzt | **nein**, unbegrenzt |
| Upgrades | **ja**, stark verbesserbar | **nein**, keine |
| Vergleichbar mit | Pipez-Rohre | Fluix-Kabel, Tesseract |

**Transportkabel** sind das, was ein Spieler aus jedem Rohr-Mod kennt. Sie führen Items, Fluide, Energie, Gase oder Redstone von A nach B. Am Anfang wird eine Menge aufgenommen, am Ende dieselbe Menge abgegeben. Wie viel das ist und wie schnell, bestimmen Limit und Upgrades.

**Digitale Kabel** transportieren nichts. Sie fassen mehrere Transportstrecken zu einem Netz zusammen. Sie kennen kein Limit und keine Upgrades, genau wie ein Fluix-Kabel oder ein Tesseract, weil sie gar keine Arbeit verrichten.

Der Übergang zwischen beiden Welten passiert an einem eigenen Block, dem **Coder** (siehe Stufe 3).

---

## 4. Design-Prinzipien

Diese Regeln gelten für jede Designentscheidung. Was dagegen verstößt, fliegt raus.

### 4.1 Kosten bremsen zwischen den Stufen, Upgrades innerhalb einer Stufe
Es gibt **keine künstliche Bürokratie**: keine Gerätekontingente wie die Channels von AE2, keine Reichweitengrenze, keine Entfernungskosten. Kein Gerät fällt je aus, weil ein Kontingent erschöpft ist.

Frequenzen sind davon unberührt — sie ordnen nur zu, welcher Coder oder Sender zu welchem Netz gehört, und sind unbegrenzt verfügbar.

Was es gibt, ist ein ehrliches Durchsatzlimit auf Transportkabeln — und das ist sehr stark aufrüstbar. Was eine höhere Stufe bremst, sind ihre Baukosten: eine Entscheidung *vorher*, keine Bestrafung *danach*.

Vorbild für Stufe 3 und 4 sind Tesseract und Quantum Entangloporter: keine Limits, aber teuer und zwei Blöcke pro Verbindung.

### 4.2 Handhabung ist Teil der Funktion
Der Mod darf nie mehr Zeit im Inventar und beim Zielen kosten als im Bauen.

| Regel | Adressiertes Problem |
|---|---|
| Großzügige Hitbox der Kabel | Fehlplatzierung bei sehr dünnen Kabeln |
| Kein Bauteil ist unstackbar | Inventar voller Einzelkarten |
| Einstellungen leben im Block, nicht im Item | ebenso |
| Eine Funktion braucht höchstens einen Block | Mehrteilige Bauteil-Stapel |
| Ändern mit dem Wrench, ohne Abbauen | Umbau-Frust |
| Abbauen gibt immer alles zurück, in einem Stück | Ersatzteil-Sucherei |

### 4.3 Neuer Block bei qualitativer Änderung, Upgrade bei quantitativer
Ändert sich nur eine Zahl (schneller, mehr), ist es ein **Upgrade**. Ändert sich das Konzept, ist es ein **neuer Block**.

Das verhindert zwei Fehler gleichzeitig: Rezept-Müll aus fünf fast identischen Kabelstufen, und Kabel, die plötzlich Dinge können, die nicht zu ihnen passen.

### 4.4 Es funktioniert ohne jede Einstellung
Quelle anbauen, Ziel anbauen, fertig. Filter leer bedeutet „alles". Gleiche Priorität bedeutet „der Reihe nach". Alles Weitere ist optional und wird erst entdeckt, wenn es gebraucht wird.

### 4.5 Performance ist ein Feature
Der Mod soll von Grund auf ressourcenschonend sein. Kein Pipez 2, kein Mekanism 2. Siehe Abschnitt 7.

### 4.6 Guter Nachbar
Wir liefern Schnittstellen, wir übernehmen nichts. Andere Mods sollen andocken können.

---

## 5. Die vier Ausbaustufen

Jede Stufe entfernt genau eine Einschränkung. Keine Stufe macht die vorherige wertlos.

| Stufe | Kabelart | Was wegfällt | Was bremst |
|---|---|---|---|
| **1 — Einzelkabel** | Transport | — | Typen getrennt, Durchsatzlimit, Kosten |
| **2 — Universalkabel** | Transport | Typ-Trennung | Durchsatzlimit, Kosten |
| **3 — Digitales Netz** | Transport + digital | Limit auf der Strecke, viele Einzelwege | Kosten pro Coder |
| **4 — Kabellos** | keine | Kabel | Kosten pro Anschluss |

Erzählerisch: **sichtbar und getrennt → sichtbar und gebündelt → strukturiert und entfesselt → unsichtbar.**

---

### Stufe 1 — Einzelkabel (Transportkabel)

Getrennte Kabelsorten pro Typ: Item, Fluid, Energie, Redstone, optional Gas.

**Warum getrennt und nicht gleich universal?** Weil man nichts falsch konfigurieren kann. Ein Item-Kabel transportiert Items. Es gibt keinen Modus, keinen Typ-Selektor, keine Fehlermeldung. Für Einsteiger ist das die niedrigste mögliche Hürde.

Dazu Endpunkte (Ein- und Ausgang), die an Kisten und Maschinen gesetzt werden.

**So funktioniert der Transport:** Am Eingang wird eine Menge X aufgenommen, am Ausgang wird eine Menge X abgegeben. Wie groß X ist und in welchem Abstand das passiert, ist das Durchsatzlimit der Strecke.

**Upgrades:** Durchsatzlimit, Geschwindigkeit, Anzahl gleichzeitiger Item-Typen, Filter, Priorität.

**Wichtig:** Upgrades sind zwischen allen Transportkabel-Stufen umsteckbar. Nichts wird je wertlos, und es gibt nur einen Upgrade-Satz statt mehrerer.

---

### Stufe 2 — Universalkabel (Transportkabel)

Ein einzelner, fertig gecrafteter Block, der **alle Typen gleichzeitig** führt. Das Rezept verbraucht die Kabel aus Stufe 1.

- Kein Reinstecken von Bauteilen, kein Typ-Selektor, keine Seiten-Konfiguration.
- Die Typen vermischen sich nicht: jeder bildet intern sein eigenes, getrenntes Netz. Der Spieler merkt davon nichts.
- Was tatsächlich fließt, entscheiden die Endpunkte an den Enden — nicht das Kabel.
- **Weiterhin mit Durchsatzlimit und denselben Upgrades wie Stufe 1.**

**Stufe 2 ist in der Bedienung einfacher als Stufe 1, nicht komplexer.** Das ist der richtige Zustand für ein Upgrade.

Stufe 1 bleibt relevant, weil sie günstiger ist. Wer nur Energie zum Ofen braucht, baut kein Universalkabel.

---

### Stufe 3 — Digitales Netz

Hier kommt die zweite Kabelart ins Spiel. Ein **Coder** ist der Übergang zwischen Transportkabel und digitalem Kabel.

```
Quelle → Transportkabel → Coder ⟶ digitales Kabel ⟶ Coder → Transportkabel → Ziel
```

**Das Problem, das es löst:** 20 Maschinen an Ort A, 20 Maschinen an Ort B. Mit reinen Transportkabeln braucht man viele Einzelstrecken. Mit Stufe 3 wird daraus ein Stamm mit zwei Ästen — eine Stern- oder Baum-Topologie.

**Eigenschaften des digitalen Netzes:**

- **Kein Durchsatzlimit.** Was ein Coder aufnimmt, kommt vollständig am anderen Ende an.
- **Keine Upgrades.** Es gibt nichts zu verbessern, weil es nichts bremst.
- **Keine Entfernungskosten.** Ein Block oder 500 Blöcke, identisch.
- **Keine Gerätekontingente.** Beliebig viele Coder in einem Netz.
- Ein Coder überträgt alle Typen, die das Transportkabelnetz dahinter führt. Nicht ein Coder pro Typ.

**Die Transportkabel an beiden Enden bleiben unverändert im Spiel** — mit ihrem Limit, ihren Upgrades und ihrer gewohnten Bedienung. Der Spieler lernt für Stufe 3 **genau zwei neue Blöcke**: das digitale Kabel und den Coder.

Der Sprung im Spielgefühl: Bisher war die Strecke der Engpass. Ab jetzt sind es nur noch die Endpunkte, an denen die Ware das System betritt und verlässt.

**Bewusst nicht übernommen:** SFMs zentraler Manager mit Programmiersprache. Die Konfiguration bleibt bei den Endpunkten, genau wie in Stufe 1 und 2. Die Bedienlogik ist über alle Stufen identisch.

**Kein Storage:** Man kann nicht nachschauen, was im Netz ist. Es gibt kein Terminal. Ware ist nur auf der digitalen Strecke digital und kommt am anderen Ende wieder als echte Ware aus einem echten Transportkabel.

---

### Stufe 4 — Kabellos

```
Quelle → Eingangsblock ⟶⟶ Ausgangsblock → Ziel
```

Gar keine Kabel mehr. Ein Block pro Gerät, Verbindung über Frequenz.

- **Teures Rezept, keine laufenden Kosten.** Energiekosten würden das Late-Game-Maximum reduzieren — genau das, was diese Zielgruppe nicht will.
- Die Bremse ist eingebaut: zwei teure Blöcke pro Verbindung. Das reguliert sich selbst.
- Mehrere Blöcke auf derselben Frequenz sind erlaubt. Ein Sender kann auf beliebig viele Empfänger verteilen.
- Pro Block einstellbar: senden, empfangen oder beides — und das getrennt je Typ.

**Vergleichbar mit** Tesseract und Quantum Entangloporter, mit mehr Einstellmöglichkeiten.

**Noch offen:** Ob der Stufe-4-Block ein aufrüstbares Durchsatzlimit trägt (wie ein Transportkabel-Endpunkt) oder unbegrenzt sendet (wie ein digitales Netz). Siehe Abschnitt 10.

**Stufe 3 und 4 sind keine Konkurrenten**, sondern zwei Werkzeuge für zwei Probleme:

| | Stufe 3 | Stufe 4 |
|---|---|---|
| Kabel | Transportkabel an beiden Enden, digital dazwischen | gar keine |
| Was verbindet sich | zwei Kabelnetze | zwei Geräte |
| Blöcke | ein Coderpaar pro Strecke | ein Block pro Gerät |
| Stärke | viele Teilnehmer, eine Strecke | maximale Sauberkeit vor Ort |

Ein Min-Maxer nutzt beides gleichzeitig: Stufe 4 im Maschinenraum, Stufe 3 für die Strecke zur Farm.

---

## 6. Transporttypen

| Typ | Status |
|---|---|
| **Items** | Kern |
| **Fluide** | Kern |
| **Energie** | Kern |
| **Redstone-Signale** | Kern — Übertragung von Punkt zu Punkt, optional über Frequenzen |
| **Gase / Chemicals** | Optional, nur bei installiertem Mekanism |

**Zu Redstone:** Der Mod *überträgt* Signale, er *verarbeitet* sie nicht. Kein Und/Oder, keine Bedingungen, keine Rechenoperationen. Das wäre Integrated-Dynamics-Territorium und genau das Gegenteil von intuitiv.

**Zu Gasen:** Es gibt keine neutrale Schnittstelle dafür, das ist Mekanisms eigenes System. Deshalb ein weiches Zusatzmodul: Ist Mekanism da, existiert der Typ. Ist es nicht da, existiert er nicht, und nichts bricht.

Alle Typen laufen über dieselbe Infrastruktur, in beiden Kabelarten.

---

## 7. Wie der Transport technisch funktioniert

Das ist die wichtigste technische Entscheidung des Projekts.

### Die Ware reist nicht wirklich

Bei klassischen Rohr-Mods ist jedes Item ein Objekt mit einer Position im Rohr, das jeden Tick weiterrückt. Jedes Rohrsegment rechnet mit. Bei tausenden Rohren und hunderten Items geht das auf die Serverleistung.

Hier passiert stattdessen nur eine Übergabe:

```
Nimm X Einheiten aus der Quelle
Lege X Einheiten ins Ziel
```

Die Kabel selbst rechnen **nichts**. Nur die Endpunkte handeln, in ihrem Intervall.

### Warum das Durchsatzlimit trotzdem funktioniert

Ein Limit ist **eine gespeicherte Zahl, keine laufende Berechnung**:

```
Bei Kabeländerung (selten):  → Limit der Strecke einmal ermitteln und merken
Bei jeder Übergabe (oft):    → gemerkten Wert nachschlagen, Menge deckeln
```

Ein Nachschlagen kostet nichts, und Kabeländerungen passieren nur, wenn der Spieler baut — nicht zwanzigmal pro Sekunde.

**Das Ergebnis:** Für den Spieler verhält sich ein Transportkabel wie bei Pipez. Der Server merkt trotzdem nichts davon. Genau das ist der Unterschied zwischen diesem Mod und einem klassischen Rohr-Mod.

### Warum Upgrades beliebig stark sein dürfen

Eine Übergabe kostet dasselbe, egal ob 4 oder 4 Millionen Einheiten bewegt werden. Upgrades dürfen deshalb sehr stark greifen, ohne die Serverleistung zu belasten.

Der praktische Flaschenhals ist ohnehin das Zielinventar — eine Kiste nimmt keine Million Items.

### Sichtbare Items im Kabel?

Technisch möglich, ohne die Serverleistung zu belasten: Der Server meldet einmal „Ware X, Route Y, Ankunft in Z Ticks", der Client animiert das rein optisch. Das ist aber Aufwand ohne Funktionsgewinn und steht **nicht** in der ersten Version.

### Netzwerke

- Jede zusammenhängende Kabelstrecke ist ihr **eigenes, unabhängiges Netz**. Es gibt kein Gesamtnetz der ganzen Base.
- Kabel verbinden → Netze verschmelzen. Kabel abbauen → Netze teilen sich.
- **Viele kleine Netze sind besser als ein großes**, auch für die Leistung.
- Ein Netz ohne aktive Endpunkte kostet nichts.

---

## 8. Wie Ziele gefunden werden

Das Netz führt eine Buchhaltung über Angebot und Nachfrage — ähnlich wie AE2, aber ohne dessen Gerätekontingente.

- Ein **Quellendpunkt** meldet: „ich biete an, was hier liegt."
- Ein **Zielendpunkt** meldet: „ich nehme Eisen." Oder „alles außer Erde."
- Das Netz vermittelt. Quelle und Ziel kennen einander nicht.

**Der praktische Vorteil:** Baut man eine neue Maschine an, meldet sie sich selbst an. Am anderen Ende des Netzes muss nichts umkonfiguriert werden.

**Volle Ziele melden sich ab**, bis wieder Platz ist. Damit löst sich das klassische Problem „Leitung verstopft, weil eine Kiste voll ist" von allein — das volle Ziel steht einfach nicht mehr in der Liste.

Das gilt in beiden Kabelarten gleichermaßen. Ein digitales Netz verbindet mehrere Transportnetze zu einem gemeinsamen Vermittlungsraum.

### Routing-Reihenfolge

1. **Filter** — wer kommt überhaupt in Frage?
2. **Priorität** — wer zuerst?
3. **Modus** — bei Gleichstand:

| Modus | Verhalten |
|---|---|
| **Der Reihe nach** (Standard) | erst Ziel A voll, dann B |
| **Round-Robin** | abwechselnd je Übergabe |
| **Gleichmäßig** | Menge aufteilen, alle gleich weit |

Den Modus legt der **Quellendpunkt** fest, weil der Spieler in Flussrichtung denkt. Mehrere Quellen stören sich nicht: jede führt ihre eigene Verteilung.

---

## 9. Was ausdrücklich nicht kommt

| Nicht enthalten | Warum |
|---|---|
| Storage-Netzwerk, Terminal mit Suchfeld | AE2-Territorium |
| Autocrafting | ebenso — gutes Logistik führt zu Autocrafting über die angeschlossenen Maschinen, nicht über das Logistiksystem selbst |
| Gerätekontingente (AE2-Channels) | unbeliebtestes Element bestehender Systeme |
| Upgrades für digitale Kabel | sie bremsen nichts, also gibt es nichts zu verbessern |
| Redstone-Logik, Bedingungen, Rechnen | Integrated-Dynamics-Territorium |
| Maschinen, Erze, Energieerzeugung, Dimensionen | kein Content-Creep |

---

## 10. Offene Fragen an euch

Wir stehen früh im Prozess. Genau darum das Feedback.

1. Sind vier Stufen die richtige Anzahl, oder ist eine überflüssig?
2. Stufe 1 mit getrennten Kabeln pro Typ — Einstiegshilfe oder unnötiger Zwischenschritt?
3. Ist die Trennung in Transportkabel und digitale Kabel auf Anhieb verständlich?
4. Soll das digitale Netz dimensionsübergreifend funktionieren?
5. **Soll der Stufe-4-Block ein aufrüstbares Durchsatzlimit haben wie ein Transportkabel, oder unbegrenzt senden wie ein digitales Netz?** Für ein Limit spricht, dass das Upgrade-System bis zum Ende relevant bleibt. Dagegen spricht, dass Stufe 4 sich als Endstufe „fertig" anfühlen sollte.
6. Fassaden zum Verstecken der Kabel — Komfort oder unnötiges Kleinteil?
7. Welche Mods müssen zwingend interoperabel sein?
8. Was nervt euch an eurem aktuellen Logistik-Setup am meisten?

---

*Dieses Dokument beschreibt einen Entwurf. Blocknamen, Rezepte und Zahlenwerte sind noch nicht festgelegt.*
