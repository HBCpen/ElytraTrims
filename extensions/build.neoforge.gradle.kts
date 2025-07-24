plugins {
    id("net.neoforged.moddev")
    id("elytratrims.common")
    id("elytratrims.extensions")
    id("me.modmuss50.mod-publish-plugin")
}

version = "${property("mod.version")}"
base.archivesName = "${property("mod.id")}-neoforge"

neoForge {
    version = property("deps.neoforge") as String

    mods {
        register("elytratrims-extensions") {
            sourceSet(sourceSets["main"])
        }
    }
}

tasks {
    processResources {
        exclude("**/fabric.mod.json")
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

    register("publishExtensions") {
        dependsOn("publishMods")
    }
}

publishMods {
    val mr = findProperty("publish.modrinth.key") as? String
    val cf = findProperty("publish.curseforge.key") as? String
    dryRun = mr == null || cf == null

    type = STABLE
    file = tasks.jar.map { it.archiveFile.get() }

    displayName = "Elytra Trims Extensions Neoforge ${property("mod.version")}"
    version = property("mod.version") as String
    changelog = provider { parent!!.file("CHANGELOG.md").readText() }
    modLoaders.add("neoforge")

    modrinth {
        projectId = property("publish.modrinth") as String
        accessToken = mr
        minecraftVersions.addAll(property("mod.releases").toString().split(' '))
        optional("elytra-trims")
    }

    curseforge {
        projectId = property("publish.curseforge") as String
        accessToken = cf
        minecraftVersions.addAll(property("mod.releases").toString().split(' '))
        optional("elytra-trims")
    }
}