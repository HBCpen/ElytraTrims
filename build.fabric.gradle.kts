@file:Suppress("UnstableApiUsage")

plugins {
    kotlin("jvm")
    id("fabric-loom")
    id("elytratrims.common")
    id("com.google.devtools.ksp")
    id("me.modmuss50.mod-publish-plugin")
    id("dev.kikugie.fletching-table.fabric") version "0.1.0-alpha.13"
}

version = "${property("mod.version")}+${property("deps.minecraft")}"
base.archivesName = "${property("mod.id")}-fabric"

loom {
    accessWidenerPath = rootProject.file("src/main/resources/elytratrims.accesswidener")
    runs.named("client") {
        ideConfigGenerated(true)
        runDir = "../../run"
        if (environment == "client")
            programArgs("--username=KikuGie")
    }
}

repositories {
    mavenLocal()
}

dependencies {
    minecraft("com.mojang:minecraft:${property("deps.minecraft")}")
    mappings(loom.layered {
        officialMojangMappings()
        if (hasProperty("deps.parchment"))
            parchment("org.parchmentmc.data:parchment-${property("deps.parchment")}@zip")
    })

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric-loader")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${property("deps.fabric-kotlin")}")

    val modules = listOf("transitive-access-wideners-v1", "registry-sync-v0", "resource-loader-v0")
    for (it in modules) modImplementation(fabricApi.module("fabric-$it", property("deps.fabric-api") as String))
}

java {
    withSourcesJar()
    withJavadocJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks {
    processResources {
        exclude("**/neoforge.mods.toml")
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        from(remapJar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }

    register("publishElytraTrims") {
        dependsOn("publishMods")
    }
}

fletchingTable {
    mixins.register("main") {
        mixin("default", "elytratrims.mixins.json")
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

    type = STABLE
    file = tasks.remapJar.map { it.archiveFile.get() }
    additionalFiles.from(tasks.remapSourcesJar.map { it.archiveFile.get() })

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