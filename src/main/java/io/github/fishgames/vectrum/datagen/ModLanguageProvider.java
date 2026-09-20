package io.github.fishgames.vectrum.datagen;

import com.google.gson.JsonObject;
import io.github.fishgames.vectrum.Vectrum;
import io.github.fishgames.vectrum.registry.ModBlocks;
import io.github.fishgames.vectrum.registry.ModItems;
import net.minecraft.Util;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** Schreibt {@code assets/vectrum/lang/<locale>.json}. Unterstuetzt "en_us" und "de_de". */
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
        out.accept(Util.makeDescriptionId("item", ModItems.EXAMPLE_ITEM.id()),
                german ? "Beispielgegenstand" : "Example Item");
        out.accept(Util.makeDescriptionId("block", ModBlocks.EXAMPLE_BLOCK.id()),
                german ? "Beispielblock" : "Example Block");

        out.accept(Util.makeDescriptionId("block", ModBlocks.ITEM_CABLE.id()),
                german ? "Item-Kabel" : "Item Cable");
        out.accept(Util.makeDescriptionId("block", ModBlocks.FLUID_CABLE.id()),
                german ? "Fluid-Kabel" : "Fluid Cable");
        out.accept(Util.makeDescriptionId("block", ModBlocks.ENERGY_CABLE.id()),
                german ? "Energie-Kabel" : "Energy Cable");
        out.accept(Util.makeDescriptionId("block", ModBlocks.ITEM_ENDPOINT.id()),
                german ? "Item-Endpunkt" : "Item Endpoint");
        out.accept(Util.makeDescriptionId("item", ModItems.WRENCH.id()),
                german ? "Schraubenschl\u00fcssel" : "Wrench");
        out.accept(Util.makeDescriptionId("item", ModItems.DIAGNOSTIC_TOOL.id()),
                german ? "Diagnosewerkzeug" : "Diagnostic Tool");

        // Meldungen in der Aktionsleiste
        out.accept("message." + Vectrum.MOD_ID + ".endpoint_mode",
                german ? "Seite %s: %s" : "Side %s: %s");
        out.accept("message." + Vectrum.MOD_ID + ".endpoint_link",
                german ? "Seite %s ist mit einem Kabel verbunden." : "Side %s is connected to a cable.");
        out.accept("message." + Vectrum.MOD_ID + ".network",
                german ? "Netz #%s: %s Bausteine, %s Eing\u00e4nge, %s Ausg\u00e4nge, Durchsatz %s %s pro \u00dcbergabe"
                        : "Network #%s: %s blocks, %s inputs, %s outputs, throughput %s %s per transfer");
        out.accept("message." + Vectrum.MOD_ID + ".no_inventory",
                german ? "Hier ist kein Inventar angeschlossen." : "No inventory attached here.");
        out.accept("message." + Vectrum.MOD_ID + ".network_none",
                german ? "Dieser Baustein geh\u00f6rt zu keinem Netz." : "This block is not part of a network.");

        // Einheiten
        out.accept("unit." + Vectrum.MOD_ID + ".item", german ? "Items" : "items");
        out.accept("unit." + Vectrum.MOD_ID + ".fluid", "mB");
        out.accept("unit." + Vectrum.MOD_ID + ".energy", "FE");

        // Befehle
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
                german ? "\"%s\" ist weder ein bekanntes Item noch ein bekanntes Fluid."
                        : "\"%s\" is neither a known item nor a known fluid.");
        out.accept("command." + Vectrum.MOD_ID + ".port.bad_side",
                german ? "\"%s\" ist keine Seite (down, up, north, south, west, east)."
                        : "\"%s\" is not a side (down, up, north, south, west, east).");

        // Verteilmodi und Filterbeschreibung
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

        // Seitennamen und Rollen
        out.accept("direction." + Vectrum.MOD_ID + ".down", german ? "unten" : "bottom");
        out.accept("direction." + Vectrum.MOD_ID + ".up", german ? "oben" : "top");
        out.accept("direction." + Vectrum.MOD_ID + ".north", german ? "Nord" : "north");
        out.accept("direction." + Vectrum.MOD_ID + ".south", german ? "S\u00fcd" : "south");
        out.accept("direction." + Vectrum.MOD_ID + ".west", german ? "West" : "west");
        out.accept("direction." + Vectrum.MOD_ID + ".east", german ? "Ost" : "east");
        out.accept("mode." + Vectrum.MOD_ID + ".off", german ? "Aus" : "Off");
        out.accept("mode." + Vectrum.MOD_ID + ".in", german ? "Eingang (entnimmt)" : "Input (takes items out)");
        out.accept("mode." + Vectrum.MOD_ID + ".out", german ? "Ausgang (liefert)" : "Output (delivers items)");
    }

    @Override
    public String getName() {
        return "Vectrum Languages: " + locale;
    }
}
