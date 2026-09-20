package io.github.fishgames.vectrum.registry;

import io.github.fishgames.vectrum.block.entity.EndpointBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    public static final Registered<BlockEntityType<EndpointBlockEntity>> ENDPOINT = Registration.register(
            Registries.BLOCK_ENTITY_TYPE, "item_endpoint",
            () -> BlockEntityType.Builder.<EndpointBlockEntity>of(EndpointBlockEntity::new, ModBlocks.ITEM_ENDPOINT.get()).build(null));

    private ModBlockEntities() {
    }

    public static void init() {
    }
}
