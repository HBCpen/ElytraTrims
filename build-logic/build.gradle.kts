plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
    maven("https://maven.neoforged.net/releases/") { name = "NeoForge" }
    maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
}

dependencies {
    implementation("dev.kikugie:stonecutter:0.9")
    implementation("net.fabricmc.fabric-loom-remap:net.fabricmc.fabric-loom-remap.gradle.plugin:1.15-SNAPSHOT")
    implementation("net.fabricmc.fabric-loom:net.fabricmc.fabric-loom.gradle.plugin:1.15-SNAPSHOT")
}

gradlePlugin {
    plugins {
        register("common") {
            id = "elytratrims-common"
            implementationClass = "CommonPlugin"
        }

        register("fabric") {
            id = "elytratrims-fabric"
            implementationClass = "FabricPlugin"
        }
    }
}
