@file:Suppress("UnstableApiUsage")

import me.modmuss50.mpp.ReleaseType
import net.fabricmc.loom.task.prod.ClientProductionRunTask

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

fabricApi {
    configureTests {
        createSourceSet = true
        modId = "elytratrims-test"

        eula = true
        enableGameTests = false
        enableClientGameTests = true
    }
}

loom {
    accessWidenerPath = sc.process(
        rootProject.file("src/main/resources/elytratrims.classtweaker"),
        "build/processed/elytratrims.classtweaker"
    )

    runs.named("client") {
        preferGradleTask = true
        generateRunConfig = true
        ideConfigFolder = "Fabric"
        runDirectory = rootProject.file("run")
        jvmArguments.add("-Dmixin.debug.export=true")
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

    val mainFapiModules = arrayOf("command-api-v2", "resource-loader-v1", "data-generation-api-v1", "transitive-access-wideners-v1", "registry-sync-v0")
    val testFapiModiles = arrayOf("client-gametest-api-v1")

    fapi(*mainFapiModules)
    fapi(*testFapiModiles) { "modGametestImplementation"(it) }
    fapi(*mainFapiModules, *testFapiModiles) { "productionRuntimeMods"(it) }
}

stonecutter {
    constants["trimica"] = false
    constants["iris"] = false
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

tasks {
    processResources {
        exclude("**/neoforge.mods.toml", "gen.sh")
    }

    named("runClientGameTest") {
        mustRunAfter("copyGametestFmj")
    }

    named<ProcessResources>("processGametestResources") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        finalizedBy("copyGametestFmj")
    }

    register<Sync>("copyGametestFmj") {
        from(rootProject.file("src/gametest/resources"))
        into(layout.buildDirectory.dir("resources/gametest"))
    }

    register<ClientProductionRunTask>("runProductionClientGameTest") {
        jvmArgs.add("-Dfabric.client.gametest")
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds mod jars and copies results to `build/libs/{mod version}/`"

        inputs.property("version", project.property("mod.version"))
        from(loomx.modJar.flatMap { it.archiveFile }, loomx.modSourcesJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
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