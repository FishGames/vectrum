package io.github.fishgames.vectrum.core.routing;

import io.github.fishgames.vectrum.core.network.BlockCoord;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Dünn besetzte Tabelle der Anschluss-Einstellungen: nur Seiten, die vom Standard abweichen, stehen darin. Für alle
 * anderen liefert {@link #get} {@link PortSettings#DEFAULT}. Nicht thread-sicher (Server-Thread).
 */
public final class PortSettingsTable {
    private final Map<PortKey, PortSettings> entries = new HashMap<>();

    public PortSettings get(PortKey key) {
        return entries.getOrDefault(key, PortSettings.DEFAULT);
    }

    /**
     * Setzt die Einstellungen; Standardwerte werden nicht gespeichert.
     *
     * @return {@code true}, wenn sich dadurch etwas geändert hat
     */
    public boolean set(PortKey key, PortSettings settings) {
        Objects.requireNonNull(settings, "settings");
        PortSettings before = get(key);
        if (settings.isDefault()) {
            entries.remove(key);
        } else {
            entries.put(key, settings);
        }
        return !before.equals(settings);
    }

    /** Vergisst alle Einstellungen aller Seiten dieses Bausteins (beim Abbauen). */
    public boolean clear(BlockCoord pos) {
        return entries.keySet().removeIf(key -> key.pos().equals(pos));
    }

    public int size() {
        return entries.size();
    }

    /** Alle abweichenden Einstellungen, zum Speichern. */
    public Map<PortKey, PortSettings> entries() {
        return Collections.unmodifiableMap(entries);
    }
}
