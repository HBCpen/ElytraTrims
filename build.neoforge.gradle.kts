plugins {
    kotlin("jvm")
    id("elytratrims-common")
    id("net.neoforged.moddev")
    id("com.google.devtools.ksp")
    id("me.modmuss50.mod-publish-plugin")
    id("dev.kikugie.fletching-table.neoforge") version "0.1.0-alpha.22"
}

version = "${property("mod.version")}+${property("deps.minecraft")}"
base.archivesName = "${property("mod.id")}-neoforge"

repositories {
    mavenLocal()
    maven("https://maven.bawnorton.com/releases")
}

neoForge {
    version = property("deps.neoforge") as String
    validateAccessTransformers = true

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
    jarJar("org.jetbrains.kotlin:kotlin-stdlib:${property("deps.kotlin-stdlib")}")
    compileOnly("dev.nyon:KotlinLangForge:2.10.6-k2.2.21-3.1+neoforge")
    compileOnly("com.bawnorton.trimica:trimica-neoforge:${property("deps.trimica")}")
    stonecutter.constants["sodium"] = fletchingTable.runCatching { modrinth("sodium", stonecutter.current.version) }
        .onSuccess { compileOnly(it) }
        .isSuccess
    stonecutter.constants["iris"] = fletchingTable.runCatching { modrinth("iris", stonecutter.current.version) }
        .onSuccess { compileOnly(it) }
        .isSuccess
}

stonecutter {
    constants["trimica"] = false
    constants["iris"] = false
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
    dryRun = mr == null || cf == null

    type = BETA
    file = tasks.jar.map { it.archiveFile.get() }
    additionalFiles.from(tasks.named<org.gradle.jvm.tasks.Jar>("sourcesJar").map { it.archiveFile.get() })

    displayName = "Elytra Trims Neoforge ${property("mod.version")} for ${stonecutter.current.version}"
    version = property("mod.version") as String
    changelog = provider { rootProject.file("CHANGELOG.md").readText() }
    modLoaders.add("neoforge")

    modrinth {
        projectId = property("publish.modrinth") as String
        accessToken = mr
        minecraftVersions.addAll(property("mod.releases").toString().split(' '))
        optional("kotlin-for-forge", "kotlin-lang-forge")
    }

    curseforge {
        projectId = property("publish.curseforge") as String
        accessToken = cf
        minecraftVersions.addAll(property("mod.releases").toString().split(' '))
        optional("kotlin-for-forge", "kotlinlangforge")
    }
}