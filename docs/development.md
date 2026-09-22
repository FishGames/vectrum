# Development notes

How the project is organised and how to build it. For playing the mod see the `README.md`.

Minecraft 1.20.1 (Java 17) for Fabric, Forge and NeoForge from one shared code base.

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
docs/                                   concept, decision log, development notes
src/main/java/.../block/                cables, endpoint (shared base ConduitBlock), connection states, shapes
src/main/java/.../world/                networks of a dimension (storage, link to the core)
src/main/java/.../logistics/            transport logic (who delivers where)
src/main/java/.../command/              admin commands (/vectrum ...), loader-independent
src/main/java/.../transfer/             access to storage (items, fluids, energy), shared interface (implemented per loader)
src/main/java/.../item/                 tools (wrench, diagnostic tool)
src/main/resources/assets/vectrum/      hand-written models and blockstates of the item endpoint, textures
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

Concept and decisions are in the folder `docs/`.

## Datagen

`generated/1.20.1/` contains everything the providers in `datagen/` write: lang files (en_us, de_de), blockstates and
models of all cables, cube blocks and plain items, loot tables, recipes with their advancements, block tags and the item
tags `vectrum:cables` and `vectrum:upgrades`. Run it with `./gradlew :1.20.1-fabric:runDatagen` (Fabric) or the `data`
run configuration (Forge/NeoForge) and commit the result. The gas cable exists only with Mekanism, so its recipe and loot
table live in `platforms/forge/resources` and `platforms/neoforge/resources`; its models are generated like the others.
The providers run unchanged on all three loaders.

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

## Commit messages

Short subject in the imperative, in English, one topic per commit (for example `Add filter ghost slots to the port GUI`).

## Documentation to-dos

Places where text is still to be written by hand are marked with `TODO(docs)`. Search for that word in `README.md`,
`CHANGELOG.md` and `gradle.properties`. Remove each marker when its text is done. Texts that live outside the
repository (mod page, screenshots) are listed in the markers of `README.md`.
