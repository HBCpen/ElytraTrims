import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources

@Suppress("unused")
class CommonPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = target.configure()
}

interface NeoForgeMutex : BuildService<BuildServiceParameters.None>

@Suppress("UnstableApiUsage")
private fun Project.configure() {
    repositories {
        fun strictMaven(url: String, vararg groups: String) = exclusiveContent {
            forRepository { maven(url) }
            filter { groups.forEach(::includeGroupAndSubgroups) }
        }

        strictMaven("https://maven.parchmentmc.org", "org.parchmentmc.data")
        strictMaven("https://repo.nyon.dev/releases", "dev.nyon")
        strictMaven("https://www.cursemaven.com", "CurseForge", "curse.maven")
        strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
    }

    extensions.configure<JavaPluginExtension> {
        withSourcesJar()
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }

    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-Xdoclint:none")
    }

    val processResources = tasks.named<ProcessResources>("processResources") {
        fun prop(name: String) = project.property(name).toString().also {
            inputs.property(name, it)
        }

        val props = HashMap<String, String>().apply {
            this["version"] = prop("mod.version")
            this["minecraft"] = prop("mod.compat")
        }

        filesMatching(listOf("fabric.mod.json", "META-INF/neoforge.mods.toml")) {
            expand(props)
        }
    }

    val checkLanguageFiles = tasks.register<LangCheckTask>("checkLanguageFiles") {
        group = "verification"
        allLanguageFiles.from(processResources.map { project.files(it.destinationDir).asFileTree.matching { include("assets/*/lang/*.json") } })
        baseLanguageFiles.from(processResources.map { project.files(it.destinationDir).asFileTree.matching { include("assets/*/lang/en_us.json") } })
    }

    tasks.named("check") {
        dependsOn(checkLanguageFiles)
    }

    val mutex = gradle.sharedServices.registerIfAbsent("createMinecraftArtifactsMutex", NeoForgeMutex::class.java) {
        maxParallelUsages.set(1)
    }

    tasks.named { it == "createMinecraftArtifacts" }.configureEach {
        usesService(mutex)
    }
}
