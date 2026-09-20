package io.github.fishgames.vectrum.core;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks that the core package imports nothing from Minecraft, Forge, NeoForge, Fabric or Mojang libraries. */
class CoreIsolationTest {
    private static final Pattern FORBIDDEN = Pattern.compile(
            "\\b(net\\.minecraft|net\\.minecraftforge|net\\.neoforged|net\\.fabricmc|com\\.mojang|cpw\\.mods|org\\.spongepowered)\\b");

    @Test
    void coreDoesNotUseMinecraftOrLoaderClasses() throws IOException {
        Path coreDir = findCoreDir();
        List<String> violations = new ArrayList<>();
        int files = 0;

        try (Stream<Path> paths = Files.walk(coreDir)) {
            for (Path path : (Iterable<Path>) paths.filter(p -> p.toString().endsWith(".java"))::iterator) {
                files++;
                List<String> lines = Files.readAllLines(path);
                for (int i = 0; i < lines.size(); i++) {
                    if (FORBIDDEN.matcher(lines.get(i)).find()) {
                        violations.add(coreDir.relativize(path) + ":" + (i + 1) + "  " + lines.get(i).trim());
                    }
                }
            }
        }

        assertTrue(files > 0, "no source files found in core under " + coreDir);
        assertEquals(List.of(), violations, "Core must not use Minecraft or loader classes");
    }

    /** Core source directory: system property, else searched upwards from the working directory. */
    private static Path findCoreDir() {
        String configured = System.getProperty("vectrum.coreDir");
        if (configured != null) {
            return Paths.get(configured);
        }
        Path directory = Paths.get("").toAbsolutePath();
        while (directory != null) {
            Path candidate = directory.resolve("src/main/java/io/github/fishgames/vectrum/core");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            directory = directory.getParent();
        }
        throw new IllegalStateException("Core directory not found. Set system property vectrum.coreDir.");
    }
}
