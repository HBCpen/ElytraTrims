import me.modmuss50.mpp.ReleaseType

plugins {
    `maven-publish`
    kotlin("jvm")
    alias(libs.plugins.mpp)
    alias(libs.plugins.neoforge)
    alias(ft.plugins.default)
    id("elytratrims-common")
}

version = "${property("mod.version")}+${property("deps.minecraft")}"
base.archivesName = "${property("mod.id")}-neoforge"

repositories {
    mavenLocal()
    maven("https://maven.bawnorton.com/releases")
    maven("https://maven.cassian.cc")
}

neoForge {
    version = property("deps.neo-loader") as String
    validateAccessTransformers = true
    accessTransformers.from(sc.process(
        file("../../src/main/resources/META-INF/accesstransformer.cfg"),
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

    compileOnly(ft.fabric.entrypoint)
    compileOnly("cc.cassian.rrv:reliable-recipe-viewer-neoforge:${property("deps.rvv_base")}+${findProperty("deps.rvv_mc") ?: property("deps.minecraft")}")
    fletchingTable.modrinth("iris")?.let {
        implementation(it)
        sc.constants["iris"] = true

        runtimeOnly(fletchingTable.modrinth("sodium")!!)
    }
}

stonecutter {
    constants["rvv"] = true
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
        description = "Builds mod jars and copies results to `build/libs/{mod version}/`"

        inputs.property("version", project.property("mod.version"))
        from(jar.flatMap { it.archiveFile }, named<Jar>("sourcesJar").flatMap { it.archiveFile })
        into(file("../../build/libs/${project.property("mod.version")}"))
    }
}

fletchingTable {
    mixins.configure(sourceSets.main) {
        mixin("elytratrims.mixins.json")
    }

    lang.configure(sourceSets.main) {
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
    file = tasks.jar.map { it.archiveFile.get() }
    additionalFiles.from(tasks.named<org.gradle.jvm.tasks.Jar>("sourcesJar").map { it.archiveFile.get() })

    displayName = "Elytra Trims Neoforge ${property("mod.version")} for ${sc.current.version}"
    version = sc.properties.get<String>("mod.version")
    changelog = providers.fileContents { file("../../CHANGELOG.md") }.asText
    modLoaders.add("neoforge")
    
    val versions = sc.properties.raw("mod:releases").to<List<String>>()

    if (publishMr)  modrinth {
        projectId = property("publish.modrinth_id") as String
        accessToken = mrToken
        minecraftVersions.addAll(versions)
        optional("kotlin-for-forge", "kotlin-lang-forge")
    }

    if (publishCf) curseforge {
        projectId = property("publish.curseforge_id") as String
        accessToken = cfToken
        minecraftVersions.addAll(versions)
        client = true
        optional("kotlin-for-forge", "kotlinlangforge")
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
            artifactId = "${property("mod.id")}-neoforge"
            version = "${property("mod.version")}+${property("deps.minecraft")}"

            from(components["java"])
        }
    }
}
