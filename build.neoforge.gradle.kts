plugins {
    kotlin("jvm")
    id("elytratrims.common")
    id("net.neoforged.moddev")
    id("com.google.devtools.ksp")
    id("me.modmuss50.mod-publish-plugin")
    id("dev.kikugie.fletching-table.neoforge") version "0.1.0-alpha.14"
}

version = "${property("mod.version")}+${property("deps.minecraft")}"
base.archivesName = "${property("mod.id")}-neoforge"

repositories {
    mavenLocal()
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
    implementation("dev.nyon:KotlinLangForge:2.7.1-k2.1.21-3.0+neoforge")
    fletchingTable.modrinth("sodium", stonecutter.current.version).let {
        compileOnly(it)
    }
}

tasks {
    processResources {
        exclude("**/fabric.mod.json", "**/elytratrims.accesswidener")
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

    register("publishElytraTrims") {
        dependsOn("publishMods")
    }
}

java {
    withSourcesJar()
    withJavadocJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

fletchingTable {
    mixins.register("main") {
        mixin("default", "elytratrims.mixins.json")
    }
}

publishMods {
    val mr = findProperty("publish.modrinth.key") as? String
    val cf = findProperty("publish.curseforge.key") as? String
    dryRun = mr == null || cf == null

    type = STABLE
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