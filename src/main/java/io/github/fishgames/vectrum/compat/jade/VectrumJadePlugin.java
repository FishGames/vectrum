package io.github.fishgames.vectrum.compat.jade;

import io.github.fishgames.vectrum.Vectrum;
import io.github.fishgames.vectrum.block.ConduitBlock;
import io.github.fishgames.vectrum.block.WirelessBlock;
import io.github.fishgames.vectrum.client.ProbeClient;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/** Jade: endpoint info lines under the block name. */
@WailaPlugin
public final class VectrumJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(EndpointProvider.INSTANCE, ConduitBlock.class);
        registration.registerBlockComponent(EndpointProvider.INSTANCE, WirelessBlock.class);
    }

    private enum EndpointProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            BlockHitResult hit = accessor.getHitResult();
            for (Component line : ProbeClient.lines(accessor.getPosition(), hit.getLocation(), hit.getDirection())) {
                tooltip.add(line);
            }
        }

        @Override
        public ResourceLocation getUid() {
            return Vectrum.id("endpoint");
        }
    }
}
