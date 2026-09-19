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
    // Laedt bei Bedarf automatisch das passende JDK (1.20.1 braucht Java 17).
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.kikugie.stonecutter") version "0.9.8"
}

rootProject.name = "vectrum"

stonecutter {
    create(rootProject) {
        // Ein Gradle-Projekt pro Kombination aus Minecraft-Version und Loader.
        // Der Projektname ("<mc>-<loader>") wird in build.gradle.kts wieder in seine Teile zerlegt.
        fun match(mc: String, vararg loaders: String) =
            loaders.forEach { version("$mc-$it", mc) }

        // Neue Minecraft-Version hinzufuegen: hier eine Zeile ergaenzen und
        // unter versions/<mc>-<loader>/gradle.properties die Versionen eintragen (siehe README.md).
        match("1.20.1", "fabric", "forge", "neoforge")

        vcsVersion = "1.20.1-fabric"
    }
}
