plugins {
    id("dev.kikugie.stonecutter")
    // Architectury Loom plugin (classpath only)
    id("dev.architectury.loom") version "1.17.493" apply false
}

stonecutter active "1.20.1-fabric" /* [SC] DO NOT EDIT */

// Loader constants for Stonecutter comments, e.g. //? if fabric { ... //?}
stonecutter parameters {
    constants.match(current.project.substringAfterLast('-'), "fabric", "forge", "neoforge")
}

// buildAll: builds all loaders and versions
tasks.register("buildAll") {
    group = "vectrum"
    description = "Builds the mod for all configured Minecraft versions and loaders."
    dependsOn(subprojects.map { ":${it.name}:build" })
}
