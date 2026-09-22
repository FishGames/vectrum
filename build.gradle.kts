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
    maven("https://api.modrinth.com/maven") { // Jade
        name = "Modrinth"
        content { includeGroup("maven.modrinth") }
    }
    maven("https://maven.bai.lol") { // WTHIT
        name = "Bai"
        content { includeGroup("mcp.mobius.waila") }
    }
    maven("https://maven.blamejared.com/") { // JEI
        name = "BlameJared"
        content { includeGroup("mezz.jei") }
    }
    maven("https://maven.terraformersmc.com/releases/") { // EMI
        name = "TerraformersMC"
        content { includeGroup("dev.emi") }
    }
    maven("https://maven.k-4u.nl/") { // The One Probe
        name = "K4U"
        content { includeGroup("mcjty.theoneprobe") }
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

/** Integrations with the full jar at runtime, chosen with -PruntimeMods=jade,wthit,jei,emi,top (dev runs only). */
val runtimeMods = (optProp("runtimeMods") ?: "").split(',').filter { it.isNotBlank() }

/** Tooltip mods and recipe viewers: compile-only API; full jar at runtime when named in -PruntimeMods. */
fun DependencyHandlerScope.integrations() {
    fun add(name: String, compile: List<String>, runtime: List<String>) {
        compile.forEach { "modCompileOnly"(it) { isTransitive = false } }
        if (name in runtimeMods) runtime.forEach { "modRuntimeOnly"(it) { isTransitive = false } }
    }
    val platform = if (isFabric) "fabric" else "forge"
    optProp("jade_version")?.let { add("jade", listOf("maven.modrinth:jade:$it"), listOf("maven.modrinth:jade:$it")) }
    optProp("wthit_version")?.let {
        val runtime = mutableListOf("mcp.mobius.waila:wthit:$it")
        optProp("badpackets_version")?.let { bad -> runtime.add("maven.modrinth:badpackets:$bad") } // WTHIT on Fabric needs it
        add("wthit", listOf("mcp.mobius.waila:wthit-api:$it"), runtime)
    }
    optProp("jei_version")?.let {
        add("jei", listOf("mezz.jei:jei-$mcVersion-common-api:$it", "mezz.jei:jei-$mcVersion-$platform-api:$it"),
                listOf("mezz.jei:jei-$mcVersion-$platform:$it"))
    }
    optProp("emi_version")?.let {
        add("emi", listOf("dev.emi:emi-$platform:$it:api"), listOf("dev.emi:emi-$platform:$it"))
    }
    optProp("top_version")?.let {
        add("top", listOf("mcjty.theoneprobe:theoneprobe:$it"), listOf("mcjty.theoneprobe:theoneprobe:$it"))
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

    integrations()

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
