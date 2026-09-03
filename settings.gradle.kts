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

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        maven("https://maven.kikugie.dev/snapshots")
    }

    versionCatalogs {
        create("ft") { from("dev.kikugie.fletching-table:fletching-table.catalog:0.2.0-alpha.9") }
    }
}

plugins {
    // For some reason, this plugin is crucial - do not remove
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.kikugie.stonecutter") version "0.9.8"
    id("dev.kikugie.loom-back-compat") version "0.4.1"
}

includeBuild("build-logic")
stonecutter {
    val matrix = System.getenv("MATRIX_INSTANCE")

    create(rootProject) {
        mapBuilds { _, node -> "build.${node.project.substringAfterLast('-')}.gradle.kts" }

        if (matrix != null)
            version(matrix, System.getenv("MATRIX_VERSION"))
        else {
            load(file("versions.json"))
            vcsVersion = "26.3-fabric"
        }
    }
}
