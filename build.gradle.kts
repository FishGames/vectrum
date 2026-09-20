// Shared build script of all version/loader projects ("1.20.1-fabric", "1.20.1-forge", "1.20.1-neoforge", ...).
// Properties: /gradle.properties and versions/<project>/gradle.properties.

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.kotlin.dsl.DependencyHandlerScope

plugins {
    id("java")
    id("dev.architectury.loom")
}

fun prop(key: String): String = property(key).toString()
fun optProp(key: String): String? = findProperty(key)?.toString()

// Project name: "<minecraft>-<loader>"
val (mcVersion, loader) = project.name.split('-', limit = 2)

val modId = prop("mod_id")
val javaVersion = prop("java_version").toInt()
val isFabric = loader == "fabric"

/** True when the project's Minecraft version is >= [min] (e.g. "1.20.2"). */
fun mcAtLeast(min: String): Boolean {
    val a = mcVersion.split('.').map { it.toIntOrNull() ?: 0 }
    val b = min.split('.').map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(a.size, b.size)) {
        val x = a.getOrElse(i) { 0 }
        val y = b.getOrElse(i) { 0 }
        if (x != y) return x > y
    }
    return true
}

// Platform sub-package compiled into the project: "fabric", "forge" (Forge and NeoForge up to 1.20.1), "neoforge" (NeoForge from 1.20.2)
val sourcePlatform = when {
    isFabric -> "fabric"
    loader == "neoforge" && mcAtLeast("1.20.2") -> "neoforge"
    else -> "forge"
}

// Datagen output directory per Minecraft version
val generatedDir = rootProject.file("generated/$mcVersion")

base.archivesName.set("$modId-$loader")
version = "${prop("mod_version")}+$mcVersion"
group = prop("mod_group")

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaVersion)
}

repositories {
    mavenCentral() // JUnit
    maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }
    maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
    maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
    maven("https://modmaven.dev/") { // Mekanism API
        name = "ModMaven"
        content { includeGroup("mekanism") }
    }
}

loom {
    silentMojangMappingsLicense()

    runs {
        if (isFabric) {
            create("datagen") {
                inherit(getByName("server"))
                vmArg("-Dfabric-api.datagen")
                vmArg("-Dfabric-api.datagen.output-dir=${generatedDir.absolutePath}")
                vmArg("-Dfabric-api.datagen.modid=$modId")
                runDir("build/datagen")
            }
        } else {
            maybeCreate("data").apply {
                data()
                programArg("--all")
                programArg("--mod")
                programArg(modId)
                programArg("--output")
                programArg(generatedDir.absolutePath)
            }
        }
    }
}

/** Mekanism dependency: compile-only API; full runtime jar with -PwithMekanism. */
fun DependencyHandlerScope.mekanism() {
    val version = optProp("mekanism_version") ?: return
    "modCompileOnly"("mekanism:Mekanism:$version:api")
    if (project.hasProperty("withMekanism")) {
        "modRuntimeOnly"("mekanism:Mekanism:$version")
    }
}

dependencies {
    "minecraft"("com.mojang:minecraft:$mcVersion")

    "mappings"(loom.layered {
        officialMojangMappings()
        optProp("parchment_version")?.let { parchment("org.parchmentmc.data:parchment-$mcVersion:$it@zip") }
    })

    when (loader) {
        "fabric" -> {
            "modImplementation"("net.fabricmc:fabric-loader:${prop("fabric_loader_version")}")
            "modImplementation"("net.fabricmc.fabric-api:fabric-api:${prop("fabric_api_version")}")

            // Energy: Team Reborn Energy (MIT), bundled as jar-in-jar
            "modImplementation"("teamreborn:energy:${prop("team_reborn_energy_version")}")
            "include"("teamreborn:energy:${prop("team_reborn_energy_version")}")
        }

        "forge" -> {
            "forge"("net.minecraftforge:forge:$mcVersion-${prop("forge_version")}")
            mekanism()
        }

        "neoforge" -> {
            if (mcAtLeast("1.20.2")) {
                // NeoForge platform from 1.20.2 (versions/<mc>-neoforge/gradle.properties: loom.platform=neoforge)
                "neoForge"("net.neoforged:neoforge:${prop("neoforge_version")}")
            } else {
                "forge"("net.neoforged:forge:$mcVersion-${prop("neoforge_version")}")
                mekanism()
            }
        }

        else -> error("Unknown loader: $loader")
    }

    // Tests of the Minecraft-free "core" package
    "testImplementation"(platform("org.junit:junit-bom:5.14.4"))
    "testImplementation"("org.junit.jupiter:junit-jupiter")
    "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
}

sourceSets {
    main {
        // Platform package of the current loader
        listOf("fabric", "forge", "neoforge")
            .filter { it != sourcePlatform }
            .forEach { java.exclude("**/platform/$it/**") }

        resources.srcDir(rootProject.file("platforms/$loader/resources")) // fabric.mod.json / META-INF/mods.toml
        resources.srcDir(generatedDir)                                     // Datagen output
    }
}

tasks.test {
    useJUnitPlatform()
    systemProperty("vectrum.coreDir", rootProject.file("src/main/java/io/github/fishgames/vectrum/core").absolutePath)
    testLogging {
        events("passed", "failed")
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = true // performance measurements
    }
}

val expandProps = mapOf(
    "mod_id" to modId,
    "mod_name" to prop("mod_name"),
    "mod_version" to prop("mod_version"),
    "mod_description" to prop("mod_description"),
    "mod_authors" to prop("mod_authors"),
    "mod_license" to prop("mod_license"),
    "minecraft_version" to mcVersion,
    "java_version" to javaVersion.toString(),
    "pack_format" to prop("pack_format"),
    "fabric_loader_version" to (optProp("fabric_loader_version") ?: "0"),
)

tasks.processResources {
    inputs.properties(expandProps)
    filesMatching(listOf("fabric.mod.json", "META-INF/mods.toml", "pack.mcmeta")) {
        expand(expandProps)
    }
}
