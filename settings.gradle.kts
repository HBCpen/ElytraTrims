pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    // For some reason, this plugin is crucial - do not remove
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.kikugie.stonecutter") version "0.9.2"
    id("dev.kikugie.loom-back-compat") version "0.2.1"
}

includeBuild("build-logic")
stonecutter {
    val matrix = System.getenv("MATRIX_INSTANCE")

    if (matrix != null) create(rootProject) { versions(matrix) }
    else create(rootProject, file("versions.json"))
}
