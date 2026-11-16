package io.github.fishgames.vectrum.world;

import io.github.fishgames.vectrum.core.config.ConfigFile;
import io.github.fishgames.vectrum.core.config.VectrumConfig;
import io.github.fishgames.vectrum.core.transport.TransportType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Throughput limits follow the upgrades and the configuration; old saves lose their derived entries. */
class LevelNetworksLimitsTest {
    private static final BlockPos UPGRADED = new BlockPos(1, 1, 1);
    private static final BlockPos MANUAL = new BlockPos(2, 1, 1);
    private static final BlockPos PLAIN = new BlockPos(3, 1, 1);

    @AfterEach
    void restoreDefaults() {
        VectrumConfig.set(VectrumConfig.DEFAULTS);
    }

    private static LevelNetworks load(CompoundTag tag) throws Exception {
        Method m = LevelNetworks.class.getDeclaredMethod("load", String.class, CompoundTag.class);
        m.setAccessible(true);
        return (LevelNetworks) m.invoke(null, "overworld", tag);
    }

    private static CompoundTag legacySave() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("roleDefault", 1);
        ListTag upgrades = new ListTag();
        CompoundTag entry = new CompoundTag();
        entry.putLong("pos", UPGRADED.asLong());
        entry.putIntArray("counts", new int[]{2, 0, 0, 0, 0, 0});
        upgrades.add(entry);
        tag.put("upgrades", upgrades);
        ListTag limits = new ListTag();
        CompoundTag layer = new CompoundTag();
        layer.putString("id", TransportType.ITEM.id());
        layer.putLongArray("pos", new long[]{UPGRADED.asLong(), MANUAL.asLong()});
        layer.putLongArray("values", new long[]{64, 999});
        limits.add(layer);
        tag.put("limits", limits);
        return tag;
    }

    @Test
    void oldSaveDropsDerivedLimitsAndKeepsManualOnes() throws Exception {
        LevelNetworks networks = load(legacySave());

        assertEquals(64, networks.throughput(TransportType.ITEM, UPGRADED));
        assertEquals(999, networks.throughput(TransportType.ITEM, MANUAL));
        assertEquals(4, networks.throughput(TransportType.ITEM, PLAIN));

        CompoundTag saved = networks.save(new CompoundTag());
        assertTrue(saved.contains("limitsManual"));
        ListTag limits = saved.getList("limits", 10);
        assertEquals(1, limits.size());
        assertEquals(1, limits.getCompound(0).getLongArray("pos").length);
        assertEquals(MANUAL.asLong(), limits.getCompound(0).getLongArray("pos")[0]);
    }

    @Test
    void manualLimitEqualToTheDerivedValueSurvivesANewSave() throws Exception {
        LevelNetworks first = load(legacySave());
        first.setThroughput(TransportType.ITEM, UPGRADED, 64);
        CompoundTag saved = first.save(new CompoundTag());

        LevelNetworks second = load(saved);
        assertEquals(64, second.throughput(TransportType.ITEM, UPGRADED));
        assertEquals(2, second.save(new CompoundTag()).getList("limits", 10).getCompound(0)
                .getLongArray("pos").length);
    }

    @Test
    void derivedLimitFollowsTheConfiguredTable() throws Exception {
        LevelNetworks networks = load(legacySave());
        VectrumConfig custom = ConfigFile.parse(List.of("[amount]", "item = 8", "[upgrades]",
                "throughput = [1, 2, 3]")).config();
        VectrumConfig.set(custom);

        assertEquals(24, networks.throughput(TransportType.ITEM, UPGRADED));
        assertEquals(8, networks.throughput(TransportType.ITEM, PLAIN));
        assertEquals(999, networks.throughput(TransportType.ITEM, MANUAL));
    }

    @Test
    void clearingTheManualLimitRestoresTheDerivedOne() throws Exception {
        LevelNetworks networks = load(legacySave());

        networks.clearThroughput(TransportType.ITEM, MANUAL);

        assertEquals(4, networks.throughput(TransportType.ITEM, MANUAL));
        assertFalse(networks.save(new CompoundTag()).getList("limits", 10).toString().contains("999"));
    }
}
