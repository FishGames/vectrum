package io.github.fishgames.vectrum.block;

import net.minecraft.util.StringRepresentable;

/**
 * Was ein Endpunkt an einer seiner sechs Seiten hat. Der Zustand steckt im BlockState, damit Modell und Netz
 * ihn ohne Blockentity lesen können.
 */
public enum Connection implements StringRepresentable {
    /** Nichts angeschlossen (oder Seite ausgeschaltet). */
    NONE("none"),
    /** Verbunden mit einem Kabel oder Endpunkt desselben Typs: Teil des Netzes. */
    LINK("link"),
    /** Inventar angeschlossen, Endpunkt entnimmt daraus und speist ins Netz ein (Quelle). */
    INPUT("in"),
    /** Inventar angeschlossen, Endpunkt liefert Ware aus dem Netz hinein (Ziel). */
    OUTPUT("out");

    private final String name;

    Connection(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
