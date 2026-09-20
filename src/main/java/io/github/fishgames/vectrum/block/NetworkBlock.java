package io.github.fishgames.vectrum.block;

import io.github.fishgames.vectrum.core.transport.TransportType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Ein Block, der Teil eines Vectrum-Netzes ist (Kabel oder Endpunkt). Einzelkabel (Stufe 1) und Endpunkte fuehren
 * genau einen Transporttyp, das Universalkabel (Stufe 2) mehrere; fuer jeden Typ bildet der Block einen Knoten in
 * einem eigenen, getrennten Netz an derselben Position.
 */
public interface NetworkBlock {
    /** Alle Typen, die dieser Block fuehrt (mindestens einer, feste Reihenfolge). */
    List<TransportType> transportTypes();

    /** Der erste (Haupt-)Typ, z. B. fuer Meldungen und Befehle ohne Typangabe. */
    default TransportType transportType() {
        return transportTypes().get(0);
    }

    default boolean carries(TransportType type) {
        return transportTypes().contains(type);
    }

    /** Verbinden sich ein Baustein dieses Typs und der Nachbar miteinander? */
    static boolean connects(BlockState neighbour, TransportType type) {
        return neighbour.getBlock() instanceof NetworkBlock other && other.carries(type);
    }

    /** Fuehren beide Bausteine mindestens einen gemeinsamen Typ? */
    static boolean connectsAny(BlockState neighbour, List<TransportType> types) {
        if (!(neighbour.getBlock() instanceof NetworkBlock other)) {
            return false;
        }
        for (TransportType type : types) {
            if (other.carries(type)) {
                return true;
            }
        }
        return false;
    }
}
