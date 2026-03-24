@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm")
    id("elytratrims-common")
    id("elytratrims-fabric")
    id("com.google.devtools.ksp")
    id("me.modmuss50.mod-publish-plugin")
    id("dev.kikugie.fletching-table.fabric") version "0.1.0-alpha.22"
}

version = "${property("mod.version")}+${property("deps.minecraft")}"
base.archivesName = "${property("mod.id")}-fabric"

loom {
    accessWidenerPath = sc.process(
        rootProject.file("src/main/resources/elytratrims.accesswidener"),
        "build/processed/elytratrims.accesswidener"
    )

    runs.named("client") {
        ideConfigGenerated(true)
        runDir = "../../run"
        if (environment == "client")
            programArgs("--username=KikuGie")
    }
}

repositories {
    mavenLocal()
    maven("https://maven.bawnorton.com/releases")
}

dependencies {
    fun fapi(vararg modules: String, builder: (Dependency) -> Any? = ::modImplementation) {
        for (it in modules) builder(fabricApi.module("fabric-$it", property("deps.fabric-api") as String))
    }

    minecraft("com.mojang:minecraft:${property("deps.minecraft")}")
    if (sc.current.parsed < "26") mappings(loom.officialMojangMappings())

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric-loader")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${property("deps.fabric-kotlin")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric-api")}")

    if (hasProperty("deps.trimica")) modCompileOnly("com.bawnorton.trimica:trimica-fabric:${property("deps.trimica")}")
    stonecutter.constants["sodium"] = fletchingTable.runCatching { modrinth("sodium", stonecutter.current.version) }
        .onSuccess { modCompileOnly(it); modLocalRuntime(it) }
        .isSuccess
    stonecutter.constants["iris"] = fletchingTable.runCatching { modrinth("iris", stonecutter.current.version) }
        .onSuccess { modCompileOnly(it); modLocalRuntime(it) }
        .isSuccess
}

stonecutter {
    constants["trimica"] = false
    constants["iris"] = false
}

kotlin {
    compilerOptions {
        languageVersion = KotlinVersion.KOTLIN_2_2
    }
}

tasks {
    processResources {
        exclude("**/neoforge.mods.toml", "gen.sh")
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        from(fabric.modJar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}

fletchingTable {
    mixins.register("main") {
        mixin("default", "elytratrims.mixins.json")
    }

    lang.register("main") {
        patterns.add("assets/elytratrims/lang/*.yml")
    }

    fabric {
        entrypointMappings.put("elytratrims-client", "dev.kikugie.elytratrims.api.ETClientInitializer")
        entrypointMappings.put("elytratrims-common", "dev.kikugie.elytratrims.api.ETCommonInitializer")
    }
}

publishMods {
    val mr = findProperty("publish.modrinth.key") as? String
    val cf = findProperty("publish.curseforge.key") as? String
    dryRun = mr == null || cf == null

    type = BETA
    file = fabric.modJar.map { it.archiveFile.get() }
    additionalFiles.from(fabric.modSourcesJar.map { it.archiveFile.get() })

    displayName = "Elytra Trims Fabric ${property("mod.version")} for ${stonecutter.current.version}"
    version = property("mod.version") as String
    changelog = provider { rootProject.file("CHANGELOG.md").readText() }
    modLoaders.add("fabric")

    modrinth {
        projectId = property("publish.modrinth") as String
        accessToken = mr
        minecraftVersions.addAll(property("mod.releases").toString().split(' '))
        requires("fabric-api", "fabric-language-kotlin")
    }

    curseforge {
        projectId = property("publish.curseforge") as String
        accessToken = cf
        minecraftVersions.addAll(property("mod.releases").toString().split(' '))
        requires("fabric-api", "fabric-language-kotlin")
    }
}