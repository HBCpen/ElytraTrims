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
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.kikugie.stonecutter") version "0.8-alpha.13"
}

stonecutter {
    create(rootProject) {
        fun match(version: String, vararg loaders: String) = loaders.forEach {
            version("$version-$it", version).buildscript = "build.$it.gradle.kts"
        }

        match("1.21.8", "fabric", "neoforge")
        match("1.21.10", "fabric", "neoforge")
        match("1.21.11", "fabric")
//        version("1.21.11-unobf", "1.21.11").buildscript = "build.unobf.gradle.kts"
        vcsVersion = "1.21.10-fabric"
    }
}