pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.architectury.dev/") { name = "Architectury" }
        maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
    }
}

plugins {
    // JDK toolchain resolver
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.kikugie.stonecutter") version "0.9.8"
}

rootProject.name = "vectrum"

stonecutter {
    create(rootProject) {
        // One Gradle project per Minecraft version and loader ("<mc>-<loader>")
        fun match(mc: String, vararg loaders: String) =
            loaders.forEach { version("$mc-$it", mc) }

        // Version/loader combinations
        match("1.20.1", "fabric", "forge", "neoforge")

        vcsVersion = "1.20.1-fabric"
    }
}
