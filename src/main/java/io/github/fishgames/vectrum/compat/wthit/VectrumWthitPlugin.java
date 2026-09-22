package io.github.fishgames.vectrum.compat.wthit;

import io.github.fishgames.vectrum.block.ConduitBlock;
import io.github.fishgames.vectrum.block.WirelessBlock;
import io.github.fishgames.vectrum.client.ProbeClient;
import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IClientRegistrar;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.ITooltip;
import mcp.mobius.waila.api.IWailaClientPlugin;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.HitResult;

/** WTHIT: endpoint info lines in the tooltip body. */
public final class VectrumWthitPlugin implements IWailaClientPlugin {
    @Override
    public void register(IClientRegistrar registrar) {
        registrar.body(new EndpointProvider(), ConduitBlock.class);
        registrar.body(new EndpointProvider(), WirelessBlock.class);
    }

    private static final class EndpointProvider implements IBlockComponentProvider {
        @Override
        public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
            HitResult hit = accessor.getHitResult();
            for (Component line : ProbeClient.lines(accessor.getPosition(), hit.getLocation(), accessor.getSide())) {
                tooltip.addLine(line);
            }
        }
    }
}
