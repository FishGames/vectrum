plugins {
    id("dev.kikugie.stonecutter")
    // Nur auf den Classpath legen; angewendet wird das Plugin in build.gradle.kts jedes Versionsprojekts.
    id("dev.architectury.loom") version "1.17.493" apply false
}

stonecutter active "1.20.1-fabric" /* [SC] DO NOT EDIT */

// Loader-Konstanten fuer Stonecutter-Kommentare im Quellcode, z. B.
//   //? if fabric { ... //?} else { ... //?}
stonecutter parameters {
    constants.match(current.project.substringAfterLast('-'), "fabric", "forge", "neoforge")
}

// Baut alle Loader und Versionen in einem Rutsch: ./gradlew buildAll
tasks.register("buildAll") {
    group = "vectrum"
    description = "Baut die Mod fuer alle konfigurierten Minecraft-Versionen und Loader."
    dependsOn(subprojects.map { ":${it.name}:build" })
}
