@file:Suppress("UnstableApiUsage")

plugins {
    id("fabric-loom")
    id("elytratrims.common")
    id("elytratrims.extensions")
    id("me.modmuss50.mod-publish-plugin")
}

version = "${property("mod.version")}"
base.archivesName = "${property("mod.id")}-fabric"

dependencies {
    minecraft("com.mojang:minecraft:${property("deps.minecraft")}")
    mappings(loom.officialMojangMappings())

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric-loader")}")
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

    register("publishExtensions") {
        dependsOn("publishMods")
    }
}

publishMods {
    val mr = findProperty("publish.modrinth.key") as? String
    val cf = findProperty("publish.curseforge.key") as? String
    dryRun = mr == null || cf == null

    type = STABLE
    file = tasks.remapJar.map { it.archiveFile.get() }

    displayName = "Elytra Trims Extensions Fabric ${property("mod.version")}"
    version = property("mod.version") as String
    changelog = provider { parent!!.file("CHANGELOG.md").readText() }
    modLoaders.add("fabric")

    modrinth {
        projectId = property("publish.modrinth") as String
        accessToken = mr
        minecraftVersions.addAll(property("mod.releases").toString().split(' '))
        requires("elytra-trims")
    }

    curseforge {
        projectId = property("publish.curseforge") as String
        accessToken = cf
        minecraftVersions.addAll(property("mod.releases").toString().split(' '))
        requires("elytra-trims")
    }
}