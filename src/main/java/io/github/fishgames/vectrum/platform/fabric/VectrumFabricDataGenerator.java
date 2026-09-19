package io.github.fishgames.vectrum.platform.fabric;

import io.github.fishgames.vectrum.datagen.VectrumDataGen;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public final class VectrumFabricDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        FabricDataGenerator.Pack pack = generator.createPack();
        for (VectrumDataGen.Entry entry : VectrumDataGen.PROVIDERS) {
            pack.addProvider((output, registries) -> entry.factory().create(output, registries));
        }
    }
}
