package io.github.fishgames.vectrum.core.routing;

import java.util.Objects;

/**
 * Einstellungen einer Anschlussseite. Alle Felder haben einen Standard, mit dem alles ohne Konfiguration läuft (K7).
 *
 * @param priority höher = wird als Ziel zuerst beliefert; Standard 0
 * @param mode     Verteilmodus; gilt, wenn die Seite Quelle ist
 * @param filter   gilt für Quelle (was darf raus) und Ziel (was darf rein)
 */
public record PortSettings(int priority, DistributionMode mode, ResourceFilter filter) {
    public static final PortSettings DEFAULT = new PortSettings(0, DistributionMode.DEFAULT, ResourceFilter.NONE);

    public PortSettings {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(filter, "filter");
    }

    public boolean isDefault() {
        return equals(DEFAULT);
    }

    public PortSettings withPriority(int priority) {
        return new PortSettings(priority, mode, filter);
    }

    public PortSettings withMode(DistributionMode mode) {
        return new PortSettings(priority, mode, filter);
    }

    public PortSettings withFilter(ResourceFilter filter) {
        return new PortSettings(priority, mode, filter);
    }
}
