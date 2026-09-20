package io.github.fishgames.vectrum;

import com.mojang.logging.LogUtils;
import io.github.fishgames.vectrum.registry.ModBlockEntities;
import io.github.fishgames.vectrum.registry.ModBlocks;
import io.github.fishgames.vectrum.registry.ModCreativeTabs;
import io.github.fishgames.vectrum.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

/**
 * Gemeinsamer, loaderunabhaengiger Einstiegspunkt. Die Plattform-Klassen unter
 * {@code platform/<loader>} rufen {@link #init()} auf und haengen die Registrierung
 * an den jeweiligen Loader an (siehe {@link io.github.fishgames.vectrum.registry.Registration}).
 */
public final class Vectrum {
    public static final String MOD_ID = "vectrum";
    public static final String MOD_NAME = "Vectrum";
    public static final Logger LOGGER = LogUtils.getLogger();

    private Vectrum() {
    }

    public static void init() {
        // Laedt die Klassen und legt damit alle Registrierungen in die Warteschlange.
        ModBlocks.init();
        ModItems.init();
        ModBlockEntities.init();
        ModCreativeTabs.init();

        LOGGER.info("{} initialisiert (Loader: {})", MOD_NAME, platform());
    }

    public static ResourceLocation id(String path) {
        //? if >=1.21 {
        /*return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
        *///?} else {
        return new ResourceLocation(MOD_ID, path);
        //?}
    }

    /** Name des Loaders - Beispiel fuer loaderspezifischen Code per Stonecutter-Kommentar. */
    public static String platform() {
        //? if fabric {
        return "fabric";
        //?} else if neoforge {
        /*return "neoforge";
        *///?} else {
        /*return "forge";
        *///?}
    }
}
