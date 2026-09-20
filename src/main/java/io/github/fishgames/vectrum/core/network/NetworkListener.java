package io.github.fishgames.vectrum.core.network;

import java.util.List;

/**
 * Wird vom {@link NetworkGraph} über Änderungen an den Netzen informiert. Die Routing-Schicht nutzt das später,
 * um Angebots-/Nachfrage-Register und gecachte Werte (z. B. das Durchsatzlimit) nur bei Änderungen anzupassen.
 * Alle Methoden sind optional.
 */
public interface NetworkListener {
    /** Ein neues Netz ist entstanden (z. B. erster Knoten ohne Nachbarn). */
    default void onCreated(Network network) {
    }

    /** {@code absorbed} wurde in {@code survivor} aufgenommen und existiert nicht mehr. */
    default void onMerged(Network survivor, Network absorbed) {
    }

    /**
     * {@code original} hat Knoten abgegeben. Die abgespaltenen Teile sind neue Netze; {@code original} lebt mit den
     * verbleibenden Knoten weiter.
     */
    default void onSplit(Network original, List<Network> newParts) {
    }

    /** Das Netz hat seinen letzten Knoten verloren und existiert nicht mehr. */
    default void onDissolved(Network network) {
    }
}
