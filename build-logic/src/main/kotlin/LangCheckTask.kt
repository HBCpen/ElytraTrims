import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromStream
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.VerificationException
import org.gradle.api.tasks.VerificationTask
import java.io.File

private val JSON = Json { ignoreUnknownKeys = true }

abstract class LangCheckTask : DefaultTask(), VerificationTask {
    @get:InputFiles
    abstract val allLanguageFiles: ConfigurableFileCollection

    @get:InputFiles
    abstract val baseLanguageFiles: ConfigurableFileCollection

    @TaskAction
    fun run() {
        val baseKeys = collectLanguageKeys(baseLanguageFiles)
        val languageFiles = allLanguageFiles.filter { it !in baseLanguageFiles }.groupBy { it.name }
        val missingKeysMessage = StringBuilder()
        for ((name, files) in languageFiles) {
            val missingKeys = baseKeys - collectLanguageKeys(files)
            if (missingKeys.isEmpty()) continue

            missingKeysMessage.appendLine("Keys missing from $name:")
            for (key in missingKeys) missingKeysMessage.appendLine("  - $key")
        }

        if (missingKeysMessage.isNotEmpty())
            throw VerificationException("Language files are missing translation keys.\n$missingKeysMessage")
    }

    private fun collectLanguageKeys(files: Iterable<File>): Set<String> = buildSet {
        files.flatMapTo(this) { readLanguageFile(it).keys }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun readLanguageFile(file: File): JsonObject = try {
        file.inputStream().use(JSON::decodeFromStream)
    } catch (e: RuntimeException) {
        throw RuntimeException("Failed to read $file").apply {
            initCause(e)
        }
    }
}