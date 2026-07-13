import me.modmuss50.mpp.ReleaseType

plugins {
    kotlin("jvm")
    alias(libs.plugins.ksp)
    alias(libs.plugins.mpp)
    alias(libs.plugins.neoforge)
    alias(libs.plugins.ft.neoforge)
    id("elytratrims-common")
}

version = "${property("mod.version")}+${property("deps.minecraft")}"
base.archivesName = "${property("mod.id")}-neoforge"

repositories {
    mavenLocal()
    maven("https://maven.bawnorton.com/releases")
}

neoForge {
    version = property("deps.neo-loader") as String
    validateAccessTransformers = true
    accessTransformers.from(sc.process(
        rootProject.file("src/main/resources/META-INF/accesstransformer.cfg"),
        "build/processed/elytratrims.accesstransformer.cfg"
    ))

    if (hasProperty("deps.parchment")) parchment {
        val (mc, ver) = (property("deps.parchment") as String).split(':')
        mappingsVersion = ver
        minecraftVersion = mc
    }

    runs {
        register("client") {
            gameDirectory = file("../../run/")
            client()
        }
    }

    mods {
        register("elytratrims") {
            sourceSet(sourceSets["main"])
        }
    }
}

dependencies {
    jarJar(implementation("org.jetbrains.kotlin:kotlin-stdlib:${property("deps.kotlin-stdlib")}")!!)
    jarJar(implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${property("deps.kotlin-coroutines")}")!!)
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
        exclude("**/fabric.mod.json", "**/elytratrims.accesswidener", "gen.sh")
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        from(jar.map { it.archiveFile })
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
}

publishMods {
    val mr = findProperty("publish.modrinth.key") as? String
    val cf = findProperty("publish.curseforge.key") as? String
    dryRun = mr == null || cf == null || sc.properties.getOrNull<Boolean>("publish.dry_run") == true

    type = ReleaseType.of(sc.properties["publish.status"])
    file = tasks.jar.map { it.archiveFile.get() }
    additionalFiles.from(tasks.named<org.gradle.jvm.tasks.Jar>("sourcesJar").map { it.archiveFile.get() })

    displayName = "Elytra Trims Neoforge ${property("mod.version")} for ${stonecutter.current.version}"
    version = property("mod.version") as String
    changelog = providers.fileContents(rootProject.layout.projectDirectory.file("CHANGELOG.md")).asText
    modLoaders.add("neoforge")
    
    val versions = sc.properties.raw("mod:releases").to<List<String>>()

    modrinth {
        projectId = property("publish.modrinth") as String
        accessToken = mr
        minecraftVersions.addAll(versions)
        optional("kotlin-for-forge", "kotlin-lang-forge")
    }

    curseforge {
        projectId = property("publish.curseforge") as String
        accessToken = cf
        minecraftVersions.addAll(versions)
        client = true
        optional("kotlin-for-forge", "kotlinlangforge")
    }
}