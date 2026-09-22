package io.github.fishgames.vectrum.datagen;

import com.google.gson.JsonObject;
import io.github.fishgames.vectrum.Vectrum;
import io.github.fishgames.vectrum.core.upgrade.UpgradeType;
import io.github.fishgames.vectrum.registry.ModBlocks;
import io.github.fishgames.vectrum.registry.ModItems;
import net.minecraft.Util;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** Writes {@code assets/vectrum/lang/<locale>.json} for "en_us" and "de_de". */
public final class ModLanguageProvider implements DataProvider {
    private final PackOutput.PathProvider paths;
    private final String locale;

    public ModLanguageProvider(PackOutput output, String locale) {
        this.paths = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "lang");
        this.locale = locale;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        JsonObject json = new JsonObject();
        translations((key, value) -> json.addProperty(key, value));
        return DataProvider.saveStable(cache, json, paths.json(Vectrum.id(locale)));
    }

    private void translations(BiConsumer<String, String> out) {
        boolean german = locale.equals("de_de");

        out.accept("itemGroup." + Vectrum.MOD_ID + ".main", Vectrum.MOD_NAME);

        out.accept(Util.makeDescriptionId("block", ModBlocks.ITEM_CABLE.id()),
                german ? "Item-Kabel" : "Item Cable");
        out.accept(Util.makeDescriptionId("block", ModBlocks.FLUID_CABLE.id()),
                german ? "Fluid-Kabel" : "Fluid Cable");
        out.accept(Util.makeDescriptionId("block", ModBlocks.ENERGY_CABLE.id()),
                german ? "Energie-Kabel" : "Energy Cable");
        out.accept(Util.makeDescriptionId("block", ModBlocks.DIGITAL_CABLE.id()),
                german ? "Digitales Kabel" : "Digital Cable");
        out.accept(Util.makeDescriptionId("block", ModBlocks.CODER.id()), "Coder");
        out.accept("type." + Vectrum.MOD_ID + ".digital", german ? "Digital" : "Digital");
        out.accept("message." + Vectrum.MOD_ID + ".coder_frequency",
                german ? "Frequenz %s" : "Frequency %s");
        out.accept("message." + Vectrum.MOD_ID + ".network_digital",
                german ? "Digitales Netz #%s: %s Bausteine, %s Coder, Frequenzen: %s"
                        : "Digital network #%s: %s blocks, %s coders, frequencies: %s");
        out.accept("command." + Vectrum.MOD_ID + ".frequency.show",
                german ? "Frequenz bei %s: %s" : "Frequency at %s: %s");
        out.accept("command." + Vectrum.MOD_ID + ".frequency.set",
                german ? "Frequenz bei %s auf %s gesetzt" : "Set the frequency at %s to %s");
        out.accept("command." + Vectrum.MOD_ID + ".frequency.not_a_coder",
                german ? "Bei %s steht weder ein Coder noch ein Funk-Anschluss." : "There is no coder or wireless port at %s.");
        out.accept(Util.makeDescriptionId("block", ModBlocks.WIRELESS_PORT.id()),
                german ? "Funk-Anschluss" : "Wireless Port");
        out.accept("link." + Vectrum.MOD_ID + ".off", german ? "Aus" : "Off");
        out.accept("link." + Vectrum.MOD_ID + ".receive", german ? "Empfangen" : "Receive");
        out.accept("link." + Vectrum.MOD_ID + ".send", german ? "Senden" : "Send");
        out.accept("link." + Vectrum.MOD_ID + ".both", german ? "Senden und empfangen" : "Send and receive");
        out.accept("message." + Vectrum.MOD_ID + ".wireless_mode",
                german ? "Funk-Anschluss: %s (alle Typen)" : "Wireless port: %s (all types)");
        out.accept("message." + Vectrum.MOD_ID + ".wireless_info",
                german ? "Funk: Frequenz %s, %s Bl\u00f6cke | Items: %s, Fluide: %s, Energie: %s%s"
                        : "Wireless: frequency %s, %s blocks | items: %s, fluids: %s, energy: %s%s");
        out.accept("command." + Vectrum.MOD_ID + ".wireless.show",
                german ? "Funk-Anschluss bei %s: Frequenz %s (%s Bl\u00f6cke), Items: %s, Fluide: %s, Energie: %s%s"
                        : "Wireless port at %s: frequency %s (%s blocks), items: %s, fluids: %s, energy: %s%s");
        out.accept("command." + Vectrum.MOD_ID + ".wireless.set",
                german ? "Funk-Anschluss bei %s: %s auf \"%s\" gestellt" : "Wireless port at %s: %s set to \"%s\"");
        out.accept("command." + Vectrum.MOD_ID + ".wireless.not_wireless",
                german ? "Bei %s steht kein Funk-Anschluss." : "There is no wireless port at %s.");
        out.accept("command." + Vectrum.MOD_ID + ".wireless.bad_argument",
                german ? "Typ (all, item, fluid, energy) oder Modus (off, receive, send, both) unbekannt."
                        : "Unknown type (all, item, fluid, energy) or mode (off, receive, send, both).");
        out.accept(Util.makeDescriptionId("block", ModBlocks.REDSTONE_CABLE.id()),
                german ? "Redstone-Kabel" : "Redstone Cable");
        out.accept("message." + Vectrum.MOD_ID + ".upgrade_not_supported",
                german ? "Dieser Baustein nimmt keine Upgrades." : "This block does not accept upgrades.");
        out.accept("message." + Vectrum.MOD_ID + ".network_signal",
                german ? "Redstone-Netz #%s: %s Bausteine, %s Eing\u00e4nge/Ausg\u00e4nge, Signalst\u00e4rke %s"
                        : "Redstone network #%s: %s blocks, %s inputs/outputs, signal strength %s");
        out.accept("command." + Vectrum.MOD_ID + ".signal_unsupported",
                german ? "%s: Dieser Baustein hat weder Upgrades noch ein Durchsatzlimit."
                        : "%s: This block has neither upgrades nor a throughput limit.");
        out.accept("command." + Vectrum.MOD_ID + ".port.role",
                german ? "%s, Seite %s: Rolle %s" : "%s, side %s: role %s");
        out.accept(Util.makeDescriptionId("block", ModBlocks.UNIVERSAL_CABLE.id()),
                german ? "Universalkabel" : "Universal Cable");
        out.accept("type." + Vectrum.MOD_ID + ".item", german ? "Items" : "Items");
        out.accept("type." + Vectrum.MOD_ID + ".fluid", german ? "Fluide" : "Fluids");
        out.accept("type." + Vectrum.MOD_ID + ".energy", german ? "Energie" : "Energy");
        // Gas module (Mekanism)
        out.accept("type." + Vectrum.MOD_ID + ".gas", german ? "Gase" : "Gases");
        out.accept("block." + Vectrum.MOD_ID + ".gas_cable", german ? "Gas-Kabel" : "Gas Cable");
        out.accept("command." + Vectrum.MOD_ID + ".throughput.bad_type",
                german ? "\"%s\" gibt es bei %s nicht (item, fluid oder energy, je nach Baustein)."
                        : "\"%s\" is not available at %s (item, fluid or energy, depending on the block).");
        out.accept(Util.makeDescriptionId("block", ModBlocks.ITEM_ENDPOINT.id()),
                german ? "Item-Endpunkt" : "Item Endpoint");
        out.accept(Util.makeDescriptionId("item", ModItems.WRENCH.id()),
                german ? "Schraubenschl\u00fcssel" : "Wrench");
        out.accept(Util.makeDescriptionId("item", ModItems.DIAGNOSTIC_TOOL.id()),
                german ? "Diagnosewerkzeug" : "Diagnostic Tool");

        // Action bar messages
        out.accept("message." + Vectrum.MOD_ID + ".endpoint_mode",
                german ? "Seite %s: %s" : "Side %s: %s");
        out.accept("message." + Vectrum.MOD_ID + ".link_severed",
                german ? "Seite %s getrennt: die Leitungen laufen jetzt unabhängig nebeneinander."
                        : "Side %s severed: the lines now run independently next to each other.");
        out.accept("message." + Vectrum.MOD_ID + ".link_restored",
                german ? "Seite %s wieder verbunden." : "Side %s reconnected.");
        out.accept("message." + Vectrum.MOD_ID + ".network",
                german ? "Netz #%s: %s Bausteine, %s Eing\u00e4nge, %s Ausg\u00e4nge, Durchsatz %s %s pro \u00dcbergabe"
                        : "Network #%s: %s blocks, %s inputs, %s outputs, throughput %s %s per transfer");
        out.accept("message." + Vectrum.MOD_ID + ".no_inventory",
                german ? "Hier ist kein Inventar angeschlossen." : "No inventory attached here.");
        out.accept("message." + Vectrum.MOD_ID + ".network_none",
                german ? "Dieser Baustein geh\u00f6rt zu keinem Netz." : "This block is not part of a network.");

        // Upgrades
        for (UpgradeType type : UpgradeType.VALUES) {
            out.accept(Util.makeDescriptionId("item", ModItems.upgradeId(type)), upgradeName(type, german));
            out.accept(Util.makeDescriptionId("item", ModItems.upgradeId(type)) + ".tooltip",
                    upgradeTooltip(type, german));
        }
        out.accept("item." + Vectrum.MOD_ID + ".upgrade.max",
                german ? "H\u00f6chstens %s pro Baustein" : "Up to %s per block");
        out.accept("message." + Vectrum.MOD_ID + ".upgrade_added",
                german ? "%s eingesetzt (%s von %s)" : "%s installed (%s of %s)");
        out.accept("message." + Vectrum.MOD_ID + ".upgrade_full",
                german ? "%s: Es passen nicht mehr als %s hinein." : "%s: at most %s fit in here.");
        out.accept("message." + Vectrum.MOD_ID + ".upgrade_needs_port",
                german ? "Upgrades brauchen ein angeschlossenes Inventar." : "Upgrades need a connected inventory.");
        out.accept("message." + Vectrum.MOD_ID + ".upgrades_removed",
                german ? "%s Upgrades herausgenommen." : "Removed %s upgrades.");
        out.accept("message." + Vectrum.MOD_ID + ".upgrades_none",
                german ? "Hier stecken keine Upgrades." : "There are no upgrades installed here.");
        out.accept("command." + Vectrum.MOD_ID + ".upgrade.list",
                german ? "Durchsatz %s, Tempo %s, Sorten %s, Filter %s, Priorit\u00e4t %s, Dimension %s"
                        : "throughput %s, speed %s, types %s, filter %s, priority %s, dimension %s");
        out.accept("command." + Vectrum.MOD_ID + ".upgrade.show",
                german ? "Upgrades bei %s: %s" : "Upgrades at %s: %s");
        out.accept("command." + Vectrum.MOD_ID + ".upgrade.changed",
                german ? "Bei %s: %s jetzt %s von %s" : "At %s: %s now %s of %s");
        out.accept("command." + Vectrum.MOD_ID + ".upgrade.cleared",
                german ? "Alle Upgrades bei %s entfernt" : "Removed all upgrades at %s");
        out.accept("command." + Vectrum.MOD_ID + ".upgrade.unknown_type",
                german ? "\"%s\" ist keine Upgrade-Sorte (throughput, speed, types, filter, priority, dimension)."
                        : "\"%s\" is not an upgrade type (throughput, speed, types, filter, priority, dimension).");
        out.accept("message." + Vectrum.MOD_ID + ".upgrade_wireless_only",
                german ? "%s passt nur in den Funk-Anschluss." : "%s only fits the wireless port.");
        out.accept("command." + Vectrum.MOD_ID + ".upgrade.wireless_only",
                german ? "Bei %s: Dieses Upgrade passt nur in den Funk-Anschluss." : "At %s: this upgrade only fits the wireless port.");
        out.accept("command." + Vectrum.MOD_ID + ".upgrade.not_for_wireless",
                german ? "Bei %s: Der Funk-Anschluss nimmt nur das Dimensions-Upgrade." : "At %s: the wireless port only accepts the dimension upgrade.");
        out.accept("command." + Vectrum.MOD_ID + ".port.locked",
                german ? "Bei %s fehlt das Upgrade \"%s\". Erst einsetzen, dann einstellen."
                        : "Block at %s needs the \"%s\" upgrade first.");
        out.accept("command." + Vectrum.MOD_ID + ".port.inactive",
                german ? "Hinweis: Gespeicherte Priorit\u00e4t oder Filter wirken erst, wenn das passende Upgrade steckt."
                        : "Note: stored priority or filter only take effect once the matching upgrade is installed.");

        // Units
        out.accept("unit." + Vectrum.MOD_ID + ".item", german ? "Items" : "items");
        out.accept("unit." + Vectrum.MOD_ID + ".fluid", "mB");
        out.accept("unit." + Vectrum.MOD_ID + ".energy", "FE");
        out.accept("unit." + Vectrum.MOD_ID + ".gas", "mB");

        // Commands
        out.accept("command." + Vectrum.MOD_ID + ".throughput.show",
                german ? "Durchsatzlimit bei %s: %s pro \u00dcbergabe" : "Throughput limit at %s: %s per transfer");
        out.accept("command." + Vectrum.MOD_ID + ".throughput.set",
                german ? "Durchsatzlimit bei %s auf %s gesetzt" : "Set throughput limit at %s to %s");
        out.accept("command." + Vectrum.MOD_ID + ".throughput.reset",
                german ? "Durchsatzlimit bei %s zur\u00fcckgesetzt (jetzt %s)" : "Reset throughput limit at %s (now %s)");
        out.accept("command." + Vectrum.MOD_ID + ".port.show",
                german ? "Anschluss bei %s, Seite %s: Priorit\u00e4t %s, Verteilung %s, Filter: %s"
                        : "Port at %s, side %s: priority %s, distribution %s, filter: %s");
        out.accept("command." + Vectrum.MOD_ID + ".port.priority",
                german ? "Priorit\u00e4t bei %s, Seite %s auf %s gesetzt" : "Set priority at %s, side %s to %s");
        out.accept("command." + Vectrum.MOD_ID + ".port.mode",
                german ? "Verteilung bei %s, Seite %s: %s" : "Distribution at %s, side %s: %s");
        out.accept("command." + Vectrum.MOD_ID + ".port.filter",
                german ? "Filter bei %s, Seite %s ge\u00e4ndert: %s" : "Changed filter at %s, side %s: %s");
        out.accept("command." + Vectrum.MOD_ID + ".port.reset",
                german ? "Einstellungen bei %s, Seite %s zur\u00fcckgesetzt" : "Reset settings at %s, side %s");
        out.accept("command." + Vectrum.MOD_ID + ".port.unknown_resource",
                german ? "\"%s\" ist weder ein bekanntes Item, Fluid noch Gas (Gase nur mit Mekanism)."
                        : "\"%s\" is not a known item, fluid or gas (gases need Mekanism).");
        out.accept("command." + Vectrum.MOD_ID + ".port.bad_side",
                german ? "\"%s\" ist keine Seite (down, up, north, south, west, east)."
                        : "\"%s\" is not a side (down, up, north, south, west, east).");

        // Distribution modes and filter descriptions
        out.accept("distribution." + Vectrum.MOD_ID + ".sequential",
                german ? "der Reihe nach (erstes Ziel zuerst f\u00fcllen)" : "sequential (fill the first target first)");
        out.accept("distribution." + Vectrum.MOD_ID + ".round_robin",
                german ? "reihum (abwechselnd)" : "round robin (take turns)");
        out.accept("distribution." + Vectrum.MOD_ID + ".balanced",
                german ? "ausgleichen (leerere Ziele bekommen mehr)" : "balanced (emptier targets get more)");
        out.accept("filter." + Vectrum.MOD_ID + ".none", german ? "keiner (alles passt)" : "none (everything passes)");
        out.accept("filter." + Vectrum.MOD_ID + ".whitelist", german ? "nur %s" : "only %s");
        out.accept("filter." + Vectrum.MOD_ID + ".blacklist", german ? "alles au\u00dfer %s" : "everything except %s");
        out.accept("filter." + Vectrum.MOD_ID + ".empty_whitelist_hint",
                german ? "Positivliste (leer, alles passt)" : "whitelist (empty, everything passes)");
        out.accept("filter." + Vectrum.MOD_ID + ".empty_blacklist_hint",
                german ? "Negativliste (leer, alles passt)" : "blacklist (empty, everything passes)");

        out.accept("command." + Vectrum.MOD_ID + ".not_a_network_block",
                german ? "Bei %s steht kein Kabel oder Endpunkt." : "There is no cable or endpoint at %s.");

        // Side names and roles
        out.accept("direction." + Vectrum.MOD_ID + ".down", german ? "unten" : "bottom");
        out.accept("direction." + Vectrum.MOD_ID + ".up", german ? "oben" : "top");
        out.accept("direction." + Vectrum.MOD_ID + ".north", german ? "Nord" : "north");
        out.accept("direction." + Vectrum.MOD_ID + ".south", german ? "S\u00fcd" : "south");
        out.accept("direction." + Vectrum.MOD_ID + ".west", german ? "West" : "west");
        out.accept("direction." + Vectrum.MOD_ID + ".east", german ? "Ost" : "east");
        out.accept("mode." + Vectrum.MOD_ID + ".off", german ? "Aus" : "Off");
        out.accept("mode." + Vectrum.MOD_ID + ".in", german ? "Eingang (entnimmt)" : "Input (takes items out)");
        out.accept("mode." + Vectrum.MOD_ID + ".in_signal", german ? "Eingang (liest das Signal)" : "Input (reads the signal)");
        out.accept("mode." + Vectrum.MOD_ID + ".out_signal", german ? "Ausgang (gibt das Signal ab)" : "Output (emits the signal)");
        out.accept("mode." + Vectrum.MOD_ID + ".out", german ? "Ausgang (liefert)" : "Output (delivers items)");
        diagnosis(out, german);
        gui(out, german);
    }

    private static void gui(BiConsumer<String, String> out, boolean german) {
        String prefix = "gui." + Vectrum.MOD_ID + ".";
        out.accept(prefix + "side", german ? "Seite: %s" : "Side: %s");
        out.accept(prefix + "probe_side", german ? "Seite %s: %s" : "Side %s: %s");
        out.accept(prefix + "role.off", german ? "Aus" : "Off");
        out.accept(prefix + "role.in", german ? "Eingang" : "Input");
        out.accept(prefix + "role.out", german ? "Ausgang" : "Output");
        out.accept(prefix + "role", german ? "Rolle: %s" : "Role: %s");
        out.accept(prefix + "distribution", german ? "Verteilung: %s" : "Distribution: %s");
        out.accept(prefix + "distribution.sequential", german ? "Der Reihe nach" : "Sequential");
        out.accept(prefix + "distribution.round_robin", german ? "Abwechselnd" : "Round robin");
        out.accept(prefix + "distribution.balanced", german ? "Ausgeglichen" : "Balanced");
        out.accept(prefix + "priority", german ? "Priorit\u00e4t: %s" : "Priority: %s");
        out.accept(prefix + "filter", german ? "Filter" : "Filter");
        out.accept(prefix + "whitelist", german ? "Whitelist" : "Whitelist");
        out.accept(prefix + "blacklist", german ? "Blacklist" : "Blacklist");
        out.accept(prefix + "filter_count", german ? "%s (%s)" : "%s (%s)");
        out.accept(prefix + "clear", german ? "Leeren" : "Clear");
        out.accept(prefix + "frequency", german ? "Frequenz: %s" : "Frequency: %s");
        out.accept(prefix + "link", german ? "%s: %s" : "%s: %s");
        out.accept(prefix + "copy", german ? "Kopieren" : "Copy");
        out.accept(prefix + "paste", german ? "Einf\u00fcgen" : "Paste");
        out.accept(prefix + "no_upgrades", german ? "Keine Upgrades eingebaut" : "No upgrades installed");
        out.accept(prefix + "upgrade_count", german ? "%s von %s eingebaut" : "%s of %s installed");
        out.accept(prefix + "signal", german ? "Signalst\u00e4rke: %s" : "Signal strength: %s");
        out.accept(prefix + "rate", german ? "%s: %s %s/s" : "%s: %s %s/s");
        out.accept(prefix + "rate_short", german ? "(%s %s/s)" : "(%s %s/s)");
        out.accept(prefix + "rate_limit", german ? "%s: %s von %s %s/s" : "%s: %s of %s %s/s");
        out.accept(prefix + "limit", german ? "Limit: %s pro Transfer, ein Transfer alle %s Ticks" : "Limit: %s per transfer, one transfer every %s ticks");
        out.accept(prefix + "limit_unlimited", german ? "Kein Limit pro Transfer" : "No limit per transfer");
        out.accept(prefix + "max_types", german ? "Item-Sorten pro Transfer: %s" : "Item types per transfer: %s");
        out.accept(prefix + "no_storage", german ? "An dieser Seite h\u00e4ngt kein Lager." : "No storage next to this side.");
        out.accept(prefix + "coder_hint", german ? "Coder mit gleicher Frequenz in einem digitalen Netz teilen sich ihre Transportnetze." : "Coders with the same frequency in one digital network share their transport networks.");
        out.accept(prefix + "tip.role", german ? "Klick wechselt: Ausgang, Eingang, Aus." : "Click to cycle: output, input, off.");
        out.accept(prefix + "tip.distribution", german ? "Klick \u00e4ndert, wie die Ziele bedient werden." : "Click to change how targets are served.");
        out.accept(prefix + "tip.filter_type", german ? "Klick wechselt zwischen Whitelist und Blacklist." : "Click to switch between whitelist and blacklist.");
        out.accept(prefix + "tip.clear", german ? "Entfernt alle Filtereintr\u00e4ge." : "Removes all filter entries.");
        out.accept(prefix + "tip.side", german ? "Wechselt zu einer anderen Seite." : "Switch to another side.");
        out.accept(prefix + "tip.copy", german ? "Kopiert die Einstellungen dieser Seite." : "Copies the settings of this side.");
        out.accept(prefix + "link.off", german ? "Aus" : "Off");
        out.accept(prefix + "link.receive", german ? "Empfangen" : "Receive");
        out.accept(prefix + "link.send", german ? "Senden" : "Send");
        out.accept(prefix + "link.both", german ? "Beides" : "Both");
        out.accept(prefix + "tip.paste", german ? "Wendet die kopierten Einstellungen an." : "Applies the copied settings.");
        out.accept(prefix + "tip.link", german ? "Klick wechselt: Empfangen, Senden, Beides, Aus." : "Click to cycle: receive, send, both, off.");
        out.accept(prefix + "tip.filter_slot", german ? "Mit einem Item klicken (Eimer f\u00fcr ein Fluid), um es hinzuzuf\u00fcgen. Mit leerer Hand auf einen Eintrag klicken, um ihn zu entfernen." : "Click with an item (a bucket for a fluid) to add it. Click an entry with an empty hand to remove it.");
    }

    private static void diagnosis(BiConsumer<String, String> out, boolean german) {
        String prefix = "diagnosis." + Vectrum.MOD_ID + ".";
        out.accept(prefix + "flowing", german ? "Es flie\u00dft" : "Flowing");
        out.accept(prefix + "idle", german ? "Bereit (nichts zu bewegen)" : "Ready (nothing to move)");
        out.accept(prefix + "no_network", german ? "Kein Netzwerk" : "Not in a network");
        out.accept(prefix + "side_off", german ? "Seite ist aus" : "Side is off");
        out.accept(prefix + "no_target", german ? "Kein Ziel" : "No target");
        out.accept(prefix + "no_source", german ? "Keine Quelle" : "No source");
        out.accept(prefix + "no_receiver", german ? "Kein Empf\u00e4nger" : "No receiver");
        out.accept(prefix + "dimension_locked", german
                ? "Empf\u00e4nger in anderen Dimensionen brauchen das Dimensions-Upgrade an beiden Enden"
                : "Receivers in other dimensions need the dimension upgrade at both ends");
        out.accept(prefix + "source_empty", german ? "Quelle ist leer" : "Source is empty");
        out.accept(prefix + "target_full", german ? "Ziel ist voll" : "Target is full");
        out.accept(prefix + "filter_blocks_all", german ? "Filter blockiert alles" : "Filter blocks everything");
        out.accept(prefix + "target_unloaded", german ? "Ziel in nicht geladenem Chunk" : "Target in an unloaded chunk");
        out.accept(prefix + "limit_reached", german ? "Durchsatzlimit erreicht" : "Throughput limit reached");
        out.accept("message." + Vectrum.MOD_ID + ".diagnosis_side", german ? "Seite %s (%s):" : "Side %s (%s):");
        out.accept("message." + Vectrum.MOD_ID + ".diagnosis_none",
                german ? "An dieser Seite h\u00e4ngt kein Lager." : "No storage is attached to this side.");
        out.accept("message." + Vectrum.MOD_ID + ".diagnosis_wireless", german ? "Funk-Anschluss:" : "Wireless port:");
    }

    private static String upgradeName(UpgradeType type, boolean german) {
        return switch (type) {
            case THROUGHPUT -> german ? "Durchsatz-Upgrade" : "Throughput Upgrade";
            case SPEED -> german ? "Tempo-Upgrade" : "Speed Upgrade";
            case TYPES -> german ? "Sorten-Upgrade" : "Types Upgrade";
            case FILTER -> german ? "Filter-Upgrade" : "Filter Upgrade";
            case PRIORITY -> german ? "Priorit\u00e4ts-Upgrade" : "Priority Upgrade";
            case DIMENSION -> german ? "Dimensions-Upgrade" : "Dimension Upgrade";
        };
    }

    private static String upgradeTooltip(UpgradeType type, boolean german) {
        return switch (type) {
            case THROUGHPUT -> german ? "Vervierfacht das Durchsatzlimit pro Upgrade."
                    : "Multiplies the throughput limit by four per upgrade.";
            case SPEED -> german ? "Halbiert den Abstand zwischen zwei \u00dcbergaben."
                    : "Halves the time between two transfers.";
            case TYPES -> german ? "Erlaubt eine Item-Sorte mehr pro \u00dcbergabe (ohne Upgrade: eine)."
                    : "Allows one more item type per transfer (one without upgrades).";
            case FILTER -> german ? "Schaltet den Filter frei." : "Unlocks the filter.";
            case PRIORITY -> german ? "Schaltet die Priorit\u00e4t frei." : "Unlocks priority.";
            case DIMENSION -> german ? "Funk-Anschluss: erlaubt Verbindungen in andere Dimensionen (beide Seiten brauchen es). Schleichen + Rechtsklick nimmt es heraus."
                    : "Wireless port: allows links to other dimensions (both ends need it). Sneak + right click removes it.";
        };
    }

    @Override
    public String getName() {
        return "Vectrum Languages: " + locale;
    }
}
