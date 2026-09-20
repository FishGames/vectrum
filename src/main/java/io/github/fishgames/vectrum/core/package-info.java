/**
 * Loaderunabhängiger Kern der Logistik (Netzwerke, später Routing, Durchsatz, Filter).
 *
 * <p><b>Regel:</b> Nichts in diesem Paket und seinen Unterpaketen darf Minecraft, Forge, NeoForge, Fabric oder
 * Mojang-Bibliotheken importieren. Dadurch lässt sich der Kern ohne laufendes Spiel testen. Ein Test
 * ({@code CoreIsolationTest}) prüft die Regel automatisch.
 *
 * <p>Positionen laufen über {@link io.github.fishgames.vectrum.core.network.BlockCoord} und
 * {@link io.github.fishgames.vectrum.core.network.Direction}, die Anbindung an Minecraft-Typen liegt in den
 * Plattform-Paketen.
 */
package io.github.fishgames.vectrum.core;
