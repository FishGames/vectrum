package io.github.fishgames.vectrum.world;

import io.github.fishgames.vectrum.block.ConduitBlock;
import io.github.fishgames.vectrum.block.EndpointMode;
import io.github.fishgames.vectrum.gui.EndpointKind;
import io.github.fishgames.vectrum.gui.EndpointMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Info lines of a block for tooltip mods (Jade, WTHIT, The One Probe).
 * <ul>
 *   <li>Cable side with storage: side and role, then per type the rate and the reasons.</li>
 *   <li>Redstone cable: side, role and signal strength.</li>
 *   <li>Wireless port: frequency, then per type the reasons.</li>
 *   <li>Coder: frequency.</li>
 * </ul>
 */
public final class ProbeLines {
    private ProbeLines() {
    }

    /**
     * Lines for the block at the position.
     *
     * @param hit  hit point of the look; the cable side is the storage side closest to it
     * @param face hit face, used when the block has no storage side
     */
    public static List<Component> of(ServerLevel level, BlockPos pos, Vec3 hit, Direction face) {
        List<Component> lines = new ArrayList<>();
        BlockState state = level.getBlockState(pos);
        EndpointKind kind = EndpointMenu.kindOf(state);
        if (kind == null) {
            return lines;
        }
        LevelNetworks networks = LevelNetworks.get(level);
        switch (kind) {
            case CABLE, REDSTONE -> {
                ConduitBlock conduit = (ConduitBlock) state.getBlock();
                Direction side = conduit.guiSide(level, pos, state, hit);
                if (side == null) {
                    return lines;
                }
                EndpointMode mode = conduit.effectiveMode(networks, level, pos, side);
                lines.add(Component.translatable("gui.vectrum.probe_side",
                        Component.translatable("direction.vectrum." + side.getName()),
                        Component.translatable("gui.vectrum.role." + mode.name().toLowerCase(Locale.ROOT))));
                if (kind == EndpointKind.REDSTONE) {
                    lines.add(Component.translatable("gui.vectrum.signal", networks.signalOutput(pos)));
                } else {
                    long now = level.getGameTime();
                    for (PortDiagnosis.Entry entry : PortDiagnosis.ofSide(level, pos, side)) {
                        double rate = mode == EndpointMode.IN ? networks.rate(pos, side, entry.type(), now) : 0;
                        lines.add(line(entry, rate));
                    }
                }
            }
            case WIRELESS -> {
                lines.add(Component.translatable("message.vectrum.coder_frequency",
                        WirelessRegistry.get(level).frequency(level, pos)));
                long now = level.getGameTime();
                for (PortDiagnosis.Entry entry : PortDiagnosis.ofWireless(level, pos)) {
                    double rate = 0;
                    for (Direction side : Sides.ALL) {
                        rate += networks.rate(pos, side, entry.type(), now);
                    }
                    lines.add(line(entry, rate));
                }
            }
            case CODER -> lines.add(Component.translatable("message.vectrum.coder_frequency",
                    networks.frequency(pos)));
        }
        return lines;
    }

    /** {@code <type>: <reasons> (<rate>)}, green when nothing is wrong and red otherwise. */
    private static Component line(PortDiagnosis.Entry entry, double rate) {
        MutableComponent line = PortDiagnosis.text(entry).withStyle(
                entry.hasProblem() ? ChatFormatting.RED : ChatFormatting.GREEN);
        if (rate > 0) {
            String id = entry.type().id();
            String unit = "unit.vectrum." + id.substring(id.indexOf(':') + 1);
            line.append(Component.literal(" ").append(Component.translatable("gui.vectrum.rate_short",
                    String.format(Locale.ROOT, "%.1f", rate), Component.translatable(unit)))
                    .withStyle(ChatFormatting.GRAY));
        }
        return line;
    }
}
