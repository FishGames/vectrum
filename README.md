# Vectrum

Minecraft mod for **Fabric**, **Forge** and **NeoForge** from one shared code base.
Currently supported: **Minecraft 1.20.1** (Java 17). More versions are prepared, see below.

## Layout

The project uses [Stonecutter](https://stonecutter.kikugie.dev) (several Minecraft versions from one source tree)
together with Architectury Loom (Fabric, Forge and NeoForge in the same build). Each combination of version and
loader is its own Gradle project, e.g. `1.20.1-fabric`.

```
settings.gradle.kts           List of all version/loader projects
stonecutter.gradle.kts        Control script (active version, loader constants, buildAll)
build.gradle.kts              One build script for ALL projects (branches by loader)
gradle.properties             Mod metadata (id, name, version, author, licence)
versions/<mc>-<loader>/       Dependency versions per project (loader, API, Parchment, pack_format, Mekanism API)
src/main/java/.../Vectrum.java          shared entry point
src/main/java/.../registry/             loader-independent registration (blocks, items, creative tab)
src/main/java/.../datagen/              loader-independent data generation (vanilla classes only)
src/main/java/.../core/                 network core WITHOUT Minecraft classes (graph, transport types, throughput, routing, math helpers)
src/test/java/.../core/                 automated tests for the core
docs/                                   concept, implementation prompt, decision log
src/main/java/.../block/                cables, endpoint (shared base ConduitBlock), connection states, shapes
src/main/java/.../world/                networks of a dimension (storage, link to the core)
src/main/java/.../logistics/            transport logic (who delivers where)
src/main/java/.../command/              admin commands (/vectrum ...), loader-independent
src/main/java/.../transfer/             access to storage (items, fluids, energy), shared interface (implemented per loader)
src/main/java/.../item/                 tools (wrench, diagnostic tool)
src/main/resources/assets/vectrum/      hand-written models and blockstates of cable and endpoint, textures
src/main/java/.../platform/fabric/      Fabric entry point + Fabric datagen entry point + inventory access (Transfer API)
src/main/java/.../platform/forge/       Forge entry point + inventory access (item handler), also used by NeoForge 1.20.1
src/main/java/.../platform/forge/mekanism/  Mekanism gas integration (Forge and NeoForge, loaded only with Mekanism)
platforms/<loader>/resources/           fabric.mod.json / META-INF/mods.toml
generated/<mc>/                         datagen output (included as a resource folder)
```

Up to 1.20.1, Forge and NeoForge share the same API (`net.minecraftforge.*`) and therefore the same package
`platform/forge`. For NeoForge 1.20.2 and newer a separate package `platform/neoforge` is expected; the selection
is already in `build.gradle.kts` (`sourcePlatform`).

## Loader-specific code

Differences between loaders and versions are written directly in the code with Stonecutter comments; examples are in
`Vectrum.java` and `ModCreativeTabs.java`:

```java
//? if fabric {
return "fabric";
//?} else if neoforge {
/*return "neoforge";
*///?} else {
/*return "forge";
*///?}
```

The variant currently active in the editor is set in `stonecutter.gradle.kts` (`stonecutter active ...`).
In IntelliJ it can be switched with the Gradle tasks `Set active project to ...` (group *stonecutter*).

## Common commands

| Command | Effect |
| --- | --- |
| `./gradlew :1.20.1-fabric:runClient` | Start the client with Fabric (likewise `-forge`, `-neoforge`) |
| `./gradlew :1.20.1-fabric:runDatagen` | Run datagen through Fabric, writes to `generated/1.20.1/` |
| `./gradlew :1.20.1-forge:runData` | Run datagen through Forge (or `-neoforge`) |
| `./gradlew :1.20.1-forge:build` | Build this loader only (jar in `versions/1.20.1-forge/build/libs`) |
| `./gradlew :1.20.1-forge:test` | Run the automated core tests |
| `./gradlew :1.20.1-forge:runServer -PwithMekanism` | Forge (or NeoForge) run with the full Mekanism jar added as runtime dependency |
| `./gradlew buildAll` | Build all loaders and versions |

On Windows use `gradlew.bat` instead of `./gradlew`. On first start Loom downloads Minecraft and the mappings,
which takes a few minutes.

## Core and tests

The package `core` holds the logic that needs no Minecraft: the network graph (cables and endpoints, merging and
splitting of networks), the transport types, the throughput limits, the routing (filter, priority, distribution modes)
and the math helpers. Because the core runs without Minecraft, it can be checked with plain JUnit tests
(`src/test/java`), including randomised tests against a simple reference solution and performance measurements.
The test `CoreIsolationTest` fails as soon as someone imports Minecraft or loader classes in the core. The tests run
with every loader project, e.g. `./gradlew :1.20.1-forge:test`.

Concept, task description and decisions are in the folder `docs/`.

## Datagen

`generated/1.20.1/` contains lang files (en_us, de_de), models, blockstates, loot tables, recipes and block tags for
the example block. The files were initially pre-filled by hand so that the mod is playable immediately; a datagen run
replaces them with the real output (and adds e.g. the recipe advancements). The providers are in `datagen/` and run
unchanged on all three loaders.

The textures (`src/main/resources/assets/vectrum/textures/`) are simple placeholders.

## Gas module

Optional, Forge and NeoForge 1.20.1 only, active when Mekanism is loaded. Mekanism is a compile-only dependency
(`modCompileOnly mekanism:Mekanism:<version>:api` from ModMaven, version in `versions/*/gradle.properties`) and an
optional dependency in `mods.toml`. On Fabric the module stays off.

## Adding a new Minecraft version (example 1.21.1)

1. Add a line to `settings.gradle.kts`, e.g. `match("1.21.1", "fabric", "neoforge")`.
2. Create `versions/1.21.1-<loader>/gradle.properties` per loader (template: the 1.20.1 files) and adjust
   `java_version` (21 from 1.20.5), `pack_format`, loader/API versions and `parchment_version`.
3. For NeoForge from 1.20.2, additionally set `loom.platform=neoforge`, create the package `platform/neoforge` and
   adjust the metadata file in `platforms/neoforge/resources/` (from 1.20.5 it is called `neoforge.mods.toml`).
4. Solve differences in code with Stonecutter comments, e.g. `//? if >=1.21 { ... //?}`.
5. Activate the new version in the editor with `Set active project to 1.21.1-...` and verify with `buildAll`.

## Bundled software

- Fabric jar: [Team Reborn Energy](https://github.com/TechReborn/Energy) 3.0.0 (MIT licence), bundled as jar-in-jar for energy transfer.

## Known gaps

- No mixins are included yet. To add them: create `vectrum.mixins.json`, list it under `mixins` in `fabric.mod.json`,
  and for Forge/NeoForge set `loom { forge { mixinConfig("vectrum.mixins.json") } }` in `build.gradle.kts`.
- Licence: `All Rights Reserved` (see `LICENSE`, same entry in the mod metadata).
