package io.github.fishgames.vectrum.core.throughput;

import io.github.fishgames.vectrum.core.network.BlockCoord;
import io.github.fishgames.vectrum.core.util.SaturatedMath;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Durchsatzlimit der Anschlussbausteine einer Netz-Ebene (Kernentscheidung K3, Entscheidung E1).
 *
 * <p>Das Limit ist <b>eine gespeicherte Zahl</b> pro Baustein und wird nie über das Kabelnetz berechnet:
 * <ul>
 *   <li>Ohne eigene Einstellung gilt das Grundlimit der Ebene.</li>
 *   <li>Nur Bausteine mit abweichendem Wert stehen in der Tabelle (dünn besetzt). Spätere Durchsatz-Upgrades tragen
 *       hier ihren Wert ein.</li>
 *   <li>Beim Übergeben genügt ein einziger Nachschlag ({@link #limitOf}) ohne Suche und ohne Berechnung.</li>
 * </ul>
 *
 * <p>Gerechnet wird mit {@code long}; erst an der Grenze zu fremden Schnittstellen wird auf {@code int} geklemmt
 * ({@link #budgetOf}, Kernentscheidung K5). Die Klasse ist nicht thread-sicher; Minecraft ruft sie nur aus dem
 * Server-Thread auf.
 */
public final class ThroughputLimits {
    private final long base;
    private final Map<BlockCoord, Long> overrides = new HashMap<>();

    /**
     * @param base Grundlimit für jeden Baustein ohne eigene Einstellung (Einheiten pro Übergabe), nicht negativ
     */
    public ThroughputLimits(long base) {
        if (base < 0) {
            throw new IllegalArgumentException("Grundlimit darf nicht negativ sein: " + base);
        }
        this.base = base;
    }

    /** Grundlimit der Ebene. */
    public long base() {
        return base;
    }

    /** Limit dieses Bausteins: sein eigener Wert oder das Grundlimit. Ein einziger Tabellenzugriff. */
    public long limitOf(BlockCoord pos) {
        Long own = overrides.get(pos);
        return own == null ? base : own;
    }

    /** Limit als {@code int} für fremde Schnittstellen, sicher geklemmt (nie negativ, nie übergelaufen). */
    public int budgetOf(BlockCoord pos) {
        return SaturatedMath.clampToNonNegativeInt(limitOf(pos));
    }

    /**
     * Setzt das Limit eines Bausteins. Ein Wert gleich dem Grundlimit wird nicht gespeichert.
     *
     * @return {@code true}, wenn sich das Limit dadurch geändert hat
     * @throws IllegalArgumentException bei negativem Wert
     */
    public boolean set(BlockCoord pos, long limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("Limit darf nicht negativ sein: " + limit);
        }
        long before = limitOf(pos);
        if (limit == base) {
            overrides.remove(pos);
        } else {
            overrides.put(pos, limit);
        }
        return before != limit;
    }

    /** Setzt den Baustein auf das Grundlimit zurück (z. B. beim Abbauen). */
    public boolean reset(BlockCoord pos) {
        return overrides.remove(pos) != null;
    }

    /** Anzahl der Bausteine mit eigenem Wert. */
    public int overrideCount() {
        return overrides.size();
    }

    /** Alle eigenen Werte, zum Speichern. */
    public Map<BlockCoord, Long> overrides() {
        return Collections.unmodifiableMap(overrides);
    }
}
