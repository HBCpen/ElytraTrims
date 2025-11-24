plugins {
    kotlin("jvm") version "2.2.20" apply false
    id("dev.kikugie.stonecutter")
    id("co.uzzu.dotenv.gradle") version "4.0.0"
    id("fabric-loom") version "1.13-SNAPSHOT" apply false
    id("net.neoforged.moddev") version "2.0.119" apply false
    id("me.modmuss50.mod-publish-plugin") version "1.0.+" apply false
    id("com.google.devtools.ksp") version "2.2.20-2.0.4" apply false
}

stonecutter active "1.21.11-fabric"

stonecutter parameters {
    constants {
        var identifier = node.metadata.project.substringAfterLast('-')
        if (identifier == "unobf") identifier = "fabric"

        match(identifier, "fabric", "neoforge")
    }

    replacements {
        string(current.parsed > "1.21.10") {
            replace("net.minecraft.resources.ResourceLocation", "net.minecraft.resources.Identifier")
            replace("net/minecraft/resources/ResourceLocation", "net/minecraft/resources/Identifier")
            replace("net.minecraft.client.renderer.RenderType", "net.minecraft.client.renderer.rendertype.RenderType")
            replace("net/minecraft/client/renderer/RenderType", "net/minecraft/client/renderer/rendertype/RenderType")
        }
    }
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
