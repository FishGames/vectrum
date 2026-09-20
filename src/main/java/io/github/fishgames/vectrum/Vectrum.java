package io.github.fishgames.vectrum;

import com.mojang.logging.LogUtils;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.registry.ModBlocks;
import io.github.fishgames.vectrum.registry.ModCreativeTabs;
import io.github.fishgames.vectrum.registry.ModItems;
import io.github.fishgames.vectrum.transfer.Ports;
import io.github.fishgames.vectrum.transfer.RedstonePorts;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

/** Common, loader-independent entry point. */
public final class Vectrum {
    public static final String MOD_ID = "vectrum";
    public static final String MOD_NAME = "Vectrum";
    public static final Logger LOGGER = LogUtils.getLogger();

    private Vectrum() {
    }

    public static void init() {
        ModBlocks.init();
        ModItems.init();
        ModCreativeTabs.init();
        // Redstone port finder
        Ports.setFinder(TransportType.REDSTONE, RedstonePorts::find);

        LOGGER.info("{} initialised (loader: {})", MOD_NAME, platform());
    }

    public static ResourceLocation id(String path) {
        //? if >=1.21 {
        /*return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
        *///?} else {
        return new ResourceLocation(MOD_ID, path);
        //?}
    }

    /** Loader name. */
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
