@file:Suppress("UnstableApiUsage")

import me.modmuss50.mpp.ReleaseType
import net.fabricmc.loom.task.prod.ClientProductionRunTask

plugins {
    `maven-publish`
    kotlin("jvm")
    alias(libs.plugins.mpp)
    alias(libs.plugins.loomx)
    alias(ft.plugins.default)
    alias(ft.plugins.fabric)
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
        file("../../src/main/resources/elytratrims.classtweaker"),
        "build/processed/elytratrims.classtweaker"
    )

    runs.named("client") {
        preferGradleTask = true
        generateRunConfig = true
        ideConfigFolder = "Fabric"
        runDirectory = file("../../run")
        jvmArguments.add("-Dmixin.debug.export=true")
    }
}

repositories {
    mavenLocal()
    maven("https://maven.bawnorton.com/releases")
    maven("https://api.modrinth.com/maven")
    maven("https://maven.gegy.dev/releases") {
        content { includeGroup("dev.lambdaurora.lambdynamiclights") }
    }
    mavenCentral()
}

dependencies {
    fun fapi(vararg modules: String, builder: (Dependency) -> Any? = ::modImplementation) {
        for (it in modules) builder(fabricApi.module("fabric-$it", property("deps.fabric-api") as String))
    }

    minecraft("com.mojang:minecraft:${property("deps.minecraft")}")
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric-api")}")
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric-loader")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${property("deps.fabric-kotlin")}")

    // Optional: lets glowing cosmetics light up their surroundings when LambDynamicLights is installed.
    modCompileOnly("dev.lambdaurora.lambdynamiclights:lambdynamiclights-api:${property("deps.lambdynamiclights")}")
    modLocalRuntime("maven.modrinth:lambdynamiclights:${property("deps.lambdynamiclights")}")

    // Optional: ModMenu's config button opens the cosmetic editor when ModMenu is installed.
    modCompileOnly("maven.modrinth:modmenu:${property("deps.modmenu")}")
    modLocalRuntime("maven.modrinth:modmenu:${property("deps.modmenu")}")

    fapi("client-gametest-api-v1") { "modGametestImplementation"(it) }
    fletchingTable.modrinth("iris")?.let {
        modImplementation(it)
        sc.constants["iris"] = true

        // Pinned: the latest Sodium (0.9.3-beta.4) declares itself incompatible with Iris 1.11.7 and below,
        // so the dev client refuses to start. This is 0.9.3-beta.3 for 26.3; unpin once a matching Iris is out.
        modRuntimeOnly("maven.modrinth:AANobbMI:aFVpk3tG")
    }
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

tasks {
    processResources {
        exclude("**/neoforge.mods.toml")
    }

    // LGPL-3.0 builds on GPL-3.0, so ship both texts with every jar.
    withType<Jar>().configureEach {
        from(file("../../LICENSE"), file("../../COPYING"))
    }

    named("runClientGameTest") {
        mustRunAfter("copyGametestFmj")
    }

    named<ProcessResources>("processGametestResources") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        finalizedBy("copyGametestFmj")
    }

    register<Sync>("copyGametestFmj") {
        from(file("../../src/gametest/resources"))
        into(layout.buildDirectory.dir("resources/gametest"))
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds mod jars and copies results to `build/libs/{mod version}/`"

        inputs.property("version", project.property("mod.version"))
        from(loomx.modJar.flatMap { it.archiveFile }, loomx.modSourcesJar.flatMap { it.archiveFile })
        into(file("../../build/libs/${project.property("mod.version")}"))
    }

}

fletchingTable {
    mixins.configure(sourceSets.main) {
        mixin("elytratrims.mixins.json")
    }

    lang.configure(sourceSets.main) {
    }

    fabric.configure(sourceSets.main) {
        entrypoint("elytratrims-client", "dev.kikugie.elytratrims.api.ETClientInitializer")
        entrypoint("elytratrims-common", "dev.kikugie.elytratrims.api.ETCommonInitializer")
    }
}

publishMods {
    val mrToken = providers.environmentVariable("MODRINTH_TOKEN")
    val cfToken = providers.environmentVariable("CURSEFORGE_TOKEN")

    val publishMr = sc.properties.getOrNull<Boolean>("publish.modrinth") ?: true
    val publishCf = sc.properties.getOrNull<Boolean>("publish.curseforge") ?: true
    val publishDry = sc.properties.getOrNull<Boolean>("publish.dry_run") ?: false

    dryRun = publishDry
        || (!mrToken.isPresent && publishMr)
        || (!cfToken.isPresent && publishCf)

    type = ReleaseType.of(sc.properties["publish.status"])
    file = loomx.modJar.map { it.archiveFile.get() }
    additionalFiles.from(loomx.modSourcesJar.map { it.archiveFile.get() })

    displayName = "Elytra Trims Cosmetic Fabric ${property("mod.version")} for ${sc.current.version}"
    version = sc.properties.get<String>("mod.version")
    // Only the section of the version being published, without its heading.
    val modVersion = property("mod.version") as String
    changelog = providers.fileContents { file("../../CHANGELOG.md") }.asText.map { text ->
        val lines = text.lines()
        val start = lines.indexOfFirst { it.trim() == "## $modVersion" }
        check(start >= 0) { "CHANGELOG.md has no section for $modVersion" }
        val end = lines.drop(start + 1).indexOfFirst { it.startsWith("## ") }
        lines.subList(start + 1, if (end < 0) lines.size else start + 1 + end).joinToString("\n").trim()
    }
    modLoaders.add("fabric")
    
    val versions = sc.properties.raw("mod:releases").to<List<String>>()

    if (publishMr) modrinth {
        projectId = property("publish.modrinth_id") as String
        accessToken = mrToken
        minecraftVersions.addAll(versions)
        requires("fabric-api", "fabric-language-kotlin")
        optional("lambdynamiclights", "modmenu")
        incompatible("elytra-trims")
    }

    if (publishCf) curseforge {
        projectId = property("publish.curseforge_id") as String
        accessToken = cfToken
        minecraftVersions.addAll(versions)
        client = true
        requires("fabric-api", "fabric-language-kotlin")
    }
}

publishing {
    repositories {
        val mavenUser = providers.environmentVariable("KIKUGIE_MAVEN_USER")
        val mavenPass = providers.environmentVariable("KIKUGIE_MAVEN_PASS")
        if (!mavenUser.isPresent || !mavenPass.isPresent) return@repositories

        maven("https://maven.kikugie.dev/releases") {
            name = "KikuGie"
            credentials {
                username = mavenUser.orNull
                password = mavenPass.orNull
            }
        }
    }

    publications {
        register<MavenPublication>("maven") {
            groupId = "dev.kikugie"
            artifactId = "${property("mod.id")}-fabric"
            version = "${property("mod.version")}+${property("deps.minecraft")}"

            from(components["java"])
        }
    }
}
