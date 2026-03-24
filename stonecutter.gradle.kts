plugins {
    kotlin("jvm") version "2.3.20" apply false
    id("dev.kikugie.stonecutter")
    id("co.uzzu.dotenv.gradle") version "4.0.0"
    id("net.neoforged.moddev") version "2.0.119" apply false
    id("me.modmuss50.mod-publish-plugin") version "1.0.+" apply false
    id("com.google.devtools.ksp") version "2.3.6" apply false
}

stonecutter active "26.1-fabric"

stonecutter parameters {
    constants {
        var identifier = node.metadata.project.substringAfterLast('-')
        if (identifier == "unobf") identifier = "fabric"

        match(identifier, "fabric", "neoforge")
    }

    replacements {
        string(current.parsed > "26.0") {
            replace("accessWidener v2 named", "accessWidener v2 official")
            replace("GuiGraphics", "GuiGraphicsExtractor")
            replace("net.minecraft.client.resources.model.AtlasManager", "net.minecraft.client.resources.model.sprite.AtlasManager")
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
