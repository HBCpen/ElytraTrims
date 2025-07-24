import dev.kikugie.stonecutter.data.tree.TreeBuilder

pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie" }
    }
}

plugins {
    // For some reason, this plugin is crucial - do not remove
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
    id("dev.kikugie.stonecutter") version "0.7.5"
}

stonecutter {
    fun TreeBuilder.match(version: String, vararg loaders: String) = loaders.forEach {
        vers("$version-$it", version).buildscript = "build.$it.gradle.kts"
    }

    create(rootProject) {
        match("1.21.8", "fabric", "neoforge")
        match("1.21.5", "fabric", "neoforge")
    }

    create("extensions") {
        match("1.21.5", "fabric", "neoforge")
    }
}