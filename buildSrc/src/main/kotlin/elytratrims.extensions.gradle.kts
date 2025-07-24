plugins {
    java
}

tasks {
    val process = register<Copy>("processResourcePack") {
        includeEmptyDirs = false
        from(parent!!.projectDir.resolve("src/textures"))
        into(layout.buildDirectory.dir("et-resources"))

        filesMatching("**/*.png") {
            val (mod, elytra) = path.split('/', limit = 2)
            path = "assets/$mod/textures/trims/entity/wings/$elytra"
        }
    }
    val tree = fileTree(layout.buildDirectory.dir("et-resources"))
        .builtBy(process)

    named<ProcessResources>("processResources") {
        from(tree)
    }
}