@file:Suppress("UnstableApiUsage")

import me.modmuss50.mpp.ReleaseType

plugins {
    kotlin("jvm")
    alias(libs.plugins.ksp)
    alias(libs.plugins.mpp)
    alias(libs.plugins.loomx)
    alias(libs.plugins.ft.fabric)
    id("elytratrims-common")
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
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric-loader")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${property("deps.fabric-kotlin")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric-api")}")
}

stonecutter {
    constants["trimica"] = false
    constants["iris"] = false
}

kotlin {
    jvmToolchain(25)
}

tasks {
    processResources {
        exclude("**/neoforge.mods.toml", "gen.sh")
    }
    register<Copy>("buildAndCollect") {
        group = "build"
        from(loomx.modJar.map { it.archiveFile })
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
    dryRun = mr == null || cf == null || sc.properties.getOrNull<Boolean>("publish.dry_run") == true

    type = ReleaseType.of(sc.properties["publish.status"])
    file = loomx.modJar.map { it.archiveFile.get() }
    additionalFiles.from(loomx.modSourcesJar.map { it.archiveFile.get() })

    displayName = "Elytra Trims Fabric ${property("mod.version")} for ${stonecutter.current.version}"
    version = property("mod.version") as String
    changelog = providers.fileContents(rootProject.layout.projectDirectory.file("CHANGELOG.md")).asText
    modLoaders.add("fabric")
    
    val versions = sc.properties.raw("mod:releases").to<List<String>>()

    modrinth {
        projectId = property("publish.modrinth") as String
        accessToken = mr
        minecraftVersions.addAll(versions)
        requires("fabric-api", "fabric-language-kotlin")
    }

    curseforge {
        projectId = property("publish.curseforge") as String
        accessToken = cf
        minecraftVersions.addAll(versions)
        client = true
        requires("fabric-api", "fabric-language-kotlin")
    }
}