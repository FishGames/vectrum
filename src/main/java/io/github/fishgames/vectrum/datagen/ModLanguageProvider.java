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
        out.accept(Util.makeDescriptionId("block", ModBlocks.ITEM_ENDPOINT.id()),
                german ? "Item-Endpunkt" : "Item Endpoint");
        out.accept(Util.makeDescriptionId("item", ModItems.WRENCH.id()),
                german ? "Schraubenschl\u00fcssel" : "Wrench");

        // Meldungen in der Aktionsleiste
        out.accept("message." + Vectrum.MOD_ID + ".endpoint_mode",
                german ? "Seite %s: %s" : "Side %s: %s");
        out.accept("message." + Vectrum.MOD_ID + ".endpoint_link",
                german ? "Seite %s ist mit einem Kabel verbunden." : "Side %s is connected to a cable.");
        out.accept("message." + Vectrum.MOD_ID + ".network",
                german ? "Netz #%s: %s Bausteine, %s Endpunkte" : "Network #%s: %s blocks, %s endpoints");
        out.accept("message." + Vectrum.MOD_ID + ".network_none",
                german ? "Dieser Baustein geh\u00f6rt zu keinem Netz." : "This block is not part of a network.");

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
