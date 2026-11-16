package io.github.fishgames.vectrum.world;

import io.github.fishgames.vectrum.block.EndpointMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/** Saves from before input became the default role keep their roles; later saves are not migrated again. */
class LevelNetworksRoleDefaultTest {
    private static CompoundTag layer(String id, long[] pos, byte[] data) {
        CompoundTag l = new CompoundTag();
        l.putString("id", id);
        l.putLongArray("pos", pos);
        l.putByteArray("data", data);
        return l;
    }

    private static LevelNetworks load(CompoundTag tag) throws Exception {
        Method m = LevelNetworks.class.getDeclaredMethod("load", String.class, CompoundTag.class);
        m.setAccessible(true);
        return (LevelNetworks) m.invoke(null, "overworld", tag);
    }

    @Test
    void oldSaveKeepsOutputRoles() throws Exception {
        BlockPos ep = new BlockPos(1, 2, 3), cable = new BlockPos(2, 2, 3), red = new BlockPos(5, 5, 5), chosen = new BlockPos(7, 7, 7);
        CompoundTag tag = new CompoundTag();
        ListTag layers = new ListTag();
        layers.add(layer("vectrum:item", new long[]{ep.asLong(), cable.asLong(), chosen.asLong()}, new byte[]{0x40, 0, 0x40}));
        layers.add(layer("vectrum:redstone", new long[]{red.asLong()}, new byte[]{0x40}));
        tag.put("layers", layers);
        ListTag ports = new ListTag();
        CompoundTag port = new CompoundTag();
        port.putLong("pos", chosen.asLong());
        port.putByteArray("modes", new byte[]{-1, -1, (byte) EndpointMode.SOURCE.ordinal(), -1, -1, -1});
        ports.add(port);
        tag.put("ports", ports);

        LevelNetworks n = load(tag);
        for (Direction d : Direction.values()) {
            assertEquals(EndpointMode.TARGET, n.storedMode(ep, d));
            assertNull(n.storedMode(cable, d));
            assertNull(n.storedMode(red, d));
        }
        assertEquals(EndpointMode.SOURCE, n.storedMode(chosen, Direction.NORTH));
        assertEquals(EndpointMode.TARGET, n.storedMode(chosen, Direction.UP));

        CompoundTag saved = n.save(new CompoundTag());
        assertTrue(saved.contains("roleDefault"));
        // New endpoint placed afterwards, then reload: no second migration
        LevelNetworks again = load(saved);
        assertEquals(EndpointMode.TARGET, again.storedMode(ep, Direction.UP));
        BlockPos fresh = new BlockPos(9, 9, 9);
        ListTag l2 = saved.getList("layers", 10);
        CompoundTag item = l2.getCompound(0);
        long[] old = item.getLongArray("pos");
        long[] np = java.util.Arrays.copyOf(old, old.length + 1);
        np[old.length] = fresh.asLong();
        byte[] od = item.getByteArray("data");
        byte[] nd = java.util.Arrays.copyOf(od, od.length + 1);
        nd[od.length] = 0x40;
        item.putLongArray("pos", np);
        item.putByteArray("data", nd);
        LevelNetworks third = load(saved);
        assertNull(third.storedMode(fresh, Direction.UP));
        assertEquals(EndpointMode.TARGET, third.storedMode(ep, Direction.UP));
    }

    @Test
    void emptySaveLoads() throws Exception {
        LevelNetworks n = load(new CompoundTag());
        assertNull(n.storedMode(new BlockPos(0, 0, 0), Direction.UP));
    }
}
