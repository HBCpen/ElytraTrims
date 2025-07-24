plugins {
    id("dev.kikugie.stonecutter")
    id("fabric-loom") version "1.11-SNAPSHOT" apply false
    id("net.neoforged.moddev") version "2.0.95" apply false
    id("me.modmuss50.mod-publish-plugin") version "0.8.+" apply false
}

stonecutter active "1.21.5-fabric"

stonecutter tasks {
    val ordering = versionComparator
        .thenComparingInt { if (it.metadata.project.endsWith("fabric")) 1 else 0 }

    order("publishModrinth", ordering)
    order("publishCurseforge", ordering)
}