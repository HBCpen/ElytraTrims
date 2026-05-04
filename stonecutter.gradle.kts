plugins {
    alias(libs.plugins.stonecutter)
    alias(libs.plugins.kotlin) apply false
}

val ci: String? = System.getenv("MATRIX_INSTANCE")
if (ci != null) sc active null else stonecutter active "26.1-fabric"

stonecutter parameters {
    val (version, loader) = current.project.split('-', limit = 2)
    properties.tags(version, loader)
    
    constants {
        match(loader, "fabric", "neoforge")
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
