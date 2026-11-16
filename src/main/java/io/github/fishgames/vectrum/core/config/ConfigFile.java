package io.github.fishgames.vectrum.core.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reader and writer of {@code vectrum.toml}: {@code [section]} headers, {@code key = integer} and
 * {@code key = [integer, ...]} lines, {@code #} comments.
 */
public final class ConfigFile {
    public static final String FILE_NAME = "vectrum.toml";

    private static final long MAX_AMOUNT = 2_000_000_000L;
    private static final long MAX_FACTOR = 1_000_000_000L;
    private static final int MAX_TICKS = 72_000;
    private static final int MAX_TABLE = VectrumConfig.MAX_UPGRADE_COUNT + 1;
    private static final int MAX_PAUSES = 16;

    private ConfigFile() {
    }

    /** Parsed settings and the problems found while reading. */
    public record Result(VectrumConfig config, List<String> warnings) {
    }

    /** Loads {@code dir/vectrum.toml}; writes the defaults when the file is missing. */
    public static Result load(Path dir) {
        Path file = dir.resolve(FILE_NAME);
        if (!Files.exists(file)) {
            List<String> warnings = new ArrayList<>();
            try {
                Files.createDirectories(dir);
                Files.writeString(file, render(VectrumConfig.DEFAULTS), StandardCharsets.UTF_8);
            } catch (IOException e) {
                warnings.add("Cannot write " + file + ": " + e.getMessage());
            }
            return new Result(VectrumConfig.DEFAULTS, warnings);
        }
        try {
            return parse(Files.readAllLines(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return new Result(VectrumConfig.DEFAULTS, List.of("Cannot read " + file + ": " + e.getMessage()));
        }
    }

    /** Settings written to a new file. */
    public static String render(VectrumConfig config) {
        StringBuilder out = new StringBuilder();
        out.append("[amount]\n");
        out.append("item = ").append(config.itemAmount()).append('\n');
        out.append("fluid = ").append(config.fluidAmount()).append('\n');
        out.append("energy = ").append(config.energyAmount()).append('\n');
        out.append("gas = ").append(config.gasAmount()).append('\n');
        out.append("source = ").append(config.sourceAmount()).append('\n');
        out.append("heat = ").append(config.heatAmount()).append("\n\n");
        out.append("[upgrades]\n");
        out.append("throughput = ").append(list(config.throughputTable())).append('\n');
        out.append("speed = ").append(list(config.speedTable())).append('\n');
        out.append("item_types = ").append(list(config.itemTypesTable())).append("\n\n");
        out.append("[wireless]\n");
        out.append("interval = ").append(config.wirelessInterval()).append("\n\n");
        out.append("[redstone]\n");
        out.append("interval = ").append(config.redstoneInterval()).append("\n\n");
        out.append("[performance]\n");
        out.append("idle_interval_max = ").append(config.idleIntervalMax()).append('\n');
        out.append("target_pause = ").append(list(config.targetPauseTable())).append('\n');
        out.append("time_budget_micros = ").append(config.timeBudgetMicros()).append('\n');
        out.append("run_budget_micros = ").append(config.runBudgetMicros()).append('\n');
        out.append("refresh_interval = ").append(config.refreshInterval()).append('\n');
        return out.toString();
    }

    /** Parses file lines; missing or invalid entries keep their default and add a warning. */
    public static Result parse(List<String> lines) {
        List<String> warnings = new ArrayList<>();
        Map<String, String> values = new HashMap<>();
        String section = "";
        for (int i = 0; i < lines.size(); i++) {
            String line = stripComment(lines.get(i)).trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("[") && line.endsWith("]") && !line.contains("=")) {
                section = line.substring(1, line.length() - 1).trim();
                continue;
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                warnings.add("Line " + (i + 1) + " ignored: " + line);
                continue;
            }
            values.put(section + "." + line.substring(0, eq).trim(), line.substring(eq + 1).trim());
        }

        VectrumConfig d = VectrumConfig.DEFAULTS;
        Reader reader = new Reader(values, warnings);
        VectrumConfig config = new VectrumConfig(
                reader.number("amount.item", d.itemAmount(), 1, MAX_AMOUNT),
                reader.number("amount.fluid", d.fluidAmount(), 1, MAX_AMOUNT),
                reader.number("amount.energy", d.energyAmount(), 1, MAX_AMOUNT),
                reader.number("amount.gas", d.gasAmount(), 1, MAX_AMOUNT),
                reader.number("amount.source", d.sourceAmount(), 1, MAX_AMOUNT),
                reader.number("amount.heat", d.heatAmount(), 1, MAX_AMOUNT),
                reader.table("upgrades.throughput", d.throughputTable(), 1, MAX_FACTOR, MAX_TABLE),
                toInts(reader.table("upgrades.speed", toLongs(d.speedTable()), 1, MAX_TICKS, MAX_TABLE)),
                toInts(reader.table("upgrades.item_types", toLongs(d.itemTypesTable()), 1, 1024, MAX_TABLE)),
                (int) reader.number("wireless.interval", d.wirelessInterval(), 1, MAX_TICKS),
                (int) reader.number("redstone.interval", d.redstoneInterval(), 1, MAX_TICKS),
                (int) reader.number("performance.idle_interval_max", d.idleIntervalMax(), 0, MAX_TICKS),
                toInts(reader.table("performance.target_pause", toLongs(d.targetPauseTable()), 1, MAX_TICKS,
                        MAX_PAUSES)),
                reader.number("performance.time_budget_micros", d.timeBudgetMicros(), 0, 1_000_000),
                reader.number("performance.run_budget_micros", d.runBudgetMicros(), 0, 1_000_000),
                (int) reader.number("performance.refresh_interval", d.refreshInterval(), 0, MAX_TICKS));
        reader.reportUnknown();
        return new Result(config, warnings);
    }

    private static String stripComment(String line) {
        int hash = line.indexOf('#');
        return hash < 0 ? line : line.substring(0, hash);
    }

    private static String list(long[] values) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            out.append(i == 0 ? "" : ", ").append(values[i]);
        }
        return out.append(']').toString();
    }

    private static String list(int[] values) {
        return list(toLongs(values));
    }

    private static long[] toLongs(int[] values) {
        long[] out = new long[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = values[i];
        }
        return out;
    }

    private static int[] toInts(long[] values) {
        int[] out = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = (int) values[i];
        }
        return out;
    }

    /** Typed access to the raw values with range checks. */
    private static final class Reader {
        private final Map<String, String> values;
        private final List<String> warnings;
        private final Set<String> used = new HashSet<>();

        Reader(Map<String, String> values, List<String> warnings) {
            this.values = values;
            this.warnings = warnings;
        }

        long number(String key, long fallback, long min, long max) {
            String raw = take(key);
            if (raw == null) {
                return fallback;
            }
            Long parsed = parseLong(raw);
            if (parsed == null || parsed < min || parsed > max) {
                warnings.add(key + " must be a number from " + min + " to " + max + ", using " + fallback);
                return fallback;
            }
            return parsed;
        }

        long[] table(String key, long[] fallback, long min, long max, int maxEntries) {
            String raw = take(key);
            if (raw == null) {
                return fallback;
            }
            long[] parsed = parseList(raw, min, max, maxEntries);
            if (parsed == null) {
                warnings.add(key + " must be a list of 1 to " + maxEntries + " numbers from " + min + " to " + max
                        + ", using the default");
                return fallback;
            }
            return parsed;
        }

        void reportUnknown() {
            for (String key : values.keySet()) {
                if (!used.contains(key)) {
                    warnings.add("Unknown setting " + key + " ignored");
                }
            }
        }

        private String take(String key) {
            used.add(key);
            return values.get(key);
        }

        private static Long parseLong(String raw) {
            try {
                return Long.parseLong(raw.replace("_", ""));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        private static long[] parseList(String raw, long min, long max, int maxEntries) {
            if (!raw.startsWith("[") || !raw.endsWith("]")) {
                return null;
            }
            String body = raw.substring(1, raw.length() - 1).trim();
            if (body.isEmpty()) {
                return null;
            }
            String[] parts = body.split(",");
            if (parts.length > maxEntries) {
                return null;
            }
            long[] out = new long[parts.length];
            for (int i = 0; i < parts.length; i++) {
                Long value = parseLong(parts[i].trim());
                if (value == null || value < min || value > max) {
                    return null;
                }
                out[i] = value;
            }
            return out;
        }
    }
}
