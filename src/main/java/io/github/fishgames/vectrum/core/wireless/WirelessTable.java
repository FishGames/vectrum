package io.github.fishgames.vectrum.core.wireless;

import io.github.fishgames.vectrum.core.network.BlockCoord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Wireless ports of a world (all dimensions) with frequency and mode per transport type, indexed by frequency. */
public final class WirelessTable {
    private static final Comparator<BlockCoord> ORDER = Comparator
            .comparing(BlockCoord::dimension).thenComparingInt(BlockCoord::x).thenComparingInt(BlockCoord::y)
            .thenComparingInt(BlockCoord::z);

    /** Frequency and non-default modes of a wireless port. */
    public static final class Entry {
        private int frequency;
        private final Map<String, LinkMode> modes = new TreeMap<>();

        public int frequency() {
            return frequency;
        }

        public LinkMode mode(String typeId) {
            return modes.getOrDefault(typeId, LinkMode.DEFAULT);
        }

        /** Non-default modes (type id to mode). */
        public Map<String, LinkMode> customModes() {
            return Collections.unmodifiableMap(modes);
        }
    }

    private final Map<BlockCoord, Entry> entries = new HashMap<>();
    private final Map<Integer, List<BlockCoord>> byFrequency = new HashMap<>();
    private long version;

    /** Change counter. */
    public long version() {
        return version;
    }

    public boolean contains(BlockCoord pos) {
        return entries.containsKey(pos);
    }

    public int size() {
        return entries.size();
    }

    /** Registers a port with frequency 0 and default modes; no-op when present. */
    public boolean register(BlockCoord pos) {
        if (entries.containsKey(pos)) {
            return false;
        }
        entries.put(pos, new Entry());
        index(0, pos);
        version++;
        return true;
    }

    /** Registers a port with the given frequency and modes, replacing an existing entry. */
    public void restore(BlockCoord pos, int frequency, Map<String, LinkMode> modes) {
        unregister(pos);
        Entry entry = new Entry();
        entry.frequency = Math.max(0, frequency);
        for (Map.Entry<String, LinkMode> mode : modes.entrySet()) {
            if (mode.getValue() != LinkMode.DEFAULT) {
                entry.modes.put(mode.getKey(), mode.getValue());
            }
        }
        entries.put(pos, entry);
        index(entry.frequency, pos);
        version++;
    }

    public boolean unregister(BlockCoord pos) {
        Entry entry = entries.remove(pos);
        if (entry == null) {
            return false;
        }
        List<BlockCoord> members = byFrequency.get(entry.frequency);
        if (members != null) {
            members.remove(pos);
            if (members.isEmpty()) {
                byFrequency.remove(entry.frequency);
            }
        }
        version++;
        return true;
    }

    /** Frequency of the port; 0 for unknown ports. */
    public int frequency(BlockCoord pos) {
        Entry entry = entries.get(pos);
        return entry == null ? 0 : entry.frequency;
    }

    /** @return {@code true} when the frequency changed */
    public boolean setFrequency(BlockCoord pos, int frequency) {
        Entry entry = entries.get(pos);
        int value = Math.max(0, frequency);
        if (entry == null || entry.frequency == value) {
            return false;
        }
        List<BlockCoord> old = byFrequency.get(entry.frequency);
        if (old != null) {
            old.remove(pos);
            if (old.isEmpty()) {
                byFrequency.remove(entry.frequency);
            }
        }
        entry.frequency = value;
        index(value, pos);
        version++;
        return true;
    }

    public LinkMode mode(BlockCoord pos, String typeId) {
        Entry entry = entries.get(pos);
        return entry == null ? LinkMode.DEFAULT : entry.mode(typeId);
    }

    /** @return {@code true} when the mode changed */
    public boolean setMode(BlockCoord pos, String typeId, LinkMode mode) {
        Entry entry = entries.get(pos);
        if (entry == null || entry.mode(typeId) == mode) {
            return false;
        }
        if (mode == LinkMode.DEFAULT) {
            entry.modes.remove(typeId);
        } else {
            entry.modes.put(typeId, mode);
        }
        version++;
        return true;
    }

    /** Ports of the frequency sorted by dimension, then position. */
    public List<BlockCoord> members(int frequency) {
        List<BlockCoord> list = byFrequency.get(frequency);
        if (list == null) {
            return List.of();
        }
        List<BlockCoord> sorted = new ArrayList<>(list);
        sorted.sort(ORDER);
        return sorted;
    }

    /** Ports of the frequency that receive the type (RECEIVE or BOTH), sorted. */
    public List<BlockCoord> receivers(int frequency, String typeId) {
        List<BlockCoord> result = new ArrayList<>();
        for (BlockCoord member : members(frequency)) {
            if (entries.get(member).mode(typeId).receives()) {
                result.add(member);
            }
        }
        return result;
    }

    /** All entries. */
    public Map<BlockCoord, Entry> entries() {
        return Collections.unmodifiableMap(entries);
    }

    private void index(int frequency, BlockCoord pos) {
        byFrequency.computeIfAbsent(frequency, key -> new ArrayList<>()).add(pos);
    }
}
