package io.github.fishgames.vectrum.platform.forge;

import io.github.fishgames.vectrum.Vectrum;
import io.github.fishgames.vectrum.world.ProbeLines;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.ProbeMode;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.InterModComms;

import java.util.function.Function;

/** The One Probe: endpoint info lines, computed on the server. */
final class TopCompat {
    private TopCompat() {
    }

    static void register() {
        InterModComms.sendTo("theoneprobe", "getTheOneProbe", () -> (Function<ITheOneProbe, Void>) probe -> {
            probe.registerProvider(new Provider());
            return null;
        });
    }

    private static final class Provider implements IProbeInfoProvider {
        @Override
        public ResourceLocation getID() {
            return Vectrum.id("endpoint");
        }

        @Override
        public void addProbeInfo(ProbeMode mode, IProbeInfo info, Player player, Level level, BlockState state,
                                 IProbeHitData data) {
            if (level instanceof ServerLevel server) {
                for (Component line : ProbeLines.of(server, data.getPos(), data.getHitVec(), data.getSideHit())) {
                    info.mcText(line);
                }
            }
        }
    }
}
