plugins {
    kotlin("jvm") version "2.2.0" apply false
    id("dev.kikugie.stonecutter")
    id("co.uzzu.dotenv.gradle") version "4.0.0"
    id("fabric-loom") version "1.11-SNAPSHOT" apply false
    id("net.neoforged.moddev") version "2.0.105" apply false
    id("me.modmuss50.mod-publish-plugin") version "0.8.+" apply false
    id("com.google.devtools.ksp") version "2.2.0-2.0.2" apply false
}

stonecutter active "1.21.8-fabric"

stonecutter parameters {
    constants.match(node.metadata.project.substringAfterLast('-'), "fabric", "neoforge")
    filters.include("**/*.fsh", "**/*.vsh")
}

stonecutter tasks {
    val ordering = versionComparator
        .thenComparingInt { if (it.metadata.project.endsWith("fabric")) 1 else 0 }

    order("publishModrinth", ordering)
    order("publishCurseforge", ordering)
}

for (version in stonecutter.versions.map { it.version }.distinct()) tasks.register("publish$version") {
    group = "publishing"
    dependsOn(stonecutter.tasks.named("publishMods") { metadata.version == version })
}

tasks.register<PublishDiscordTask>("publishDiscord") {
    stonecutter.tree.nodes.map { "${it.project.path}:buildAndCollect" }
        .let { mustRunAfter(*it.toTypedArray()) }
    dependsOn("chiseledBuildAndCollect")

    fun prop(string: String) = project.property(string) as String
    files = objects.directoryProperty().value(project.layout.buildDirectory.dir("libs"))
    changelog = file("CHANGES.md").readText()
    version = prop("mod.version")

    token = env.fetch("DISCORD_TOKEN", "_")
    server = env.fetch("DISCORD_SERVER", "0").toLong()
    channel = env.fetch("DISCORD_CHANNEL", "0").toLong()
    role = env.fetch("DISCORD_ROLE", "0").toLong()
}
