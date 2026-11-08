package dev.kikugie.elytratrims.cosmetic

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.util.StringRepresentable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.*

/**
 * The local player's cosmetic, persisted in `config/elytratrims_cosmetic.json`.
 */
object CosmeticSettings {
    private val LOGGER: Logger = LoggerFactory.getLogger("ElytraTrims Cosmetics")
    private val GSON = GsonBuilder().setPrettyPrinting().create()
    private val path: Path get() = FabricLoader.getInstance().configDir.resolve("elytratrims_cosmetic.json")

    data class Data(
        val enabled: Boolean = true,
        val cosmetic: ElytraCosmetic = ElytraCosmetic.EMPTY,
        val designs: List<Design> = emptyList(),
        val sync: Sync = Sync(),
    ) {
        companion object {
            val CODEC: Codec<Data> = RecordCodecBuilder.create { it.group(
                Codec.BOOL.optionalFieldOf("enabled", true).forGetter(Data::enabled),
                ElytraCosmetic.CODEC.optionalFieldOf("cosmetic", ElytraCosmetic.EMPTY).forGetter(Data::cosmetic),
                Design.CODEC.listOf().optionalFieldOf("designs", emptyList()).forGetter(Data::designs),
                Sync.CODEC.optionalFieldOf("sync", Sync()).forGetter(Data::sync),
            ).apply(it, ::Data) }
        }
    }

    /**
     * Exchanging cosmetics with other players of this mod through a relay server.
     *
     * @property relayUrl base URL of the relay, see `relay/README.md`; blank disables syncing.
     */
    data class Sync(val mode: SyncMode = SyncMode.SHARE, val relayUrl: String = DEFAULT_RELAY_URL) {
        val relay: String? get() = relayUrl.trim().trimEnd('/').takeIf { it.startsWith("https://") || it.startsWith("http://") }

        companion object {
            /** The relay run for this fork, see `relay/`. */
            const val DEFAULT_RELAY_URL: String = "https://elytratrims-cosmetic-relay.elytratrims-cosmetic-relay.workers.dev"

            val CODEC: Codec<Sync> = RecordCodecBuilder.create { it.group(
                SyncMode.CODEC.optionalFieldOf("mode", SyncMode.SHARE).forGetter(Sync::mode),
                Codec.STRING.optionalFieldOf("relay_url", DEFAULT_RELAY_URL).forGetter(Sync::relayUrl),
            ).apply(it, ::Sync) }
        }
    }

    enum class SyncMode(private val serializedName: String) : StringRepresentable {
        /** Neither publishes the own cosmetic nor shows those of others. */
        OFF("off"),
        /** Shows the cosmetics of others without publishing the own one. */
        RECEIVE("receive"),
        /** Publishes the own cosmetic and shows those of others. */
        SHARE("share");

        override fun getSerializedName(): String = serializedName

        companion object {
            val CODEC: Codec<SyncMode> = StringRepresentable.fromEnum(SyncMode::values)
        }
    }

    /** A named cosmetic saved for later. */
    data class Design(val name: String, val cosmetic: ElytraCosmetic) {
        companion object {
            const val MAX_NAME_LENGTH = 32

            val CODEC: Codec<Design> = RecordCodecBuilder.create { it.group(
                Codec.string(1, MAX_NAME_LENGTH).fieldOf("name").forGetter(Design::name),
                ElytraCosmetic.CODEC.fieldOf("cosmetic").forGetter(Design::cosmetic),
            ).apply(it, ::Design) }

            private val COMPACT_GSON = GsonBuilder().disableHtmlEscaping().create()

            /** Single-line JSON, for sharing a design through the clipboard. */
            fun encode(design: Design): String = COMPACT_GSON.toJson(CODEC.encodeStart(JsonOps.INSTANCE, design).orThrow)

            /** Reads a design from [encode]d text, or `null` if the text isn't one. */
            fun decode(text: String): Design? = runCatching {
                CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(text.trim())).result().orElse(null)
            }.getOrNull()
        }
    }

    @Volatile var data: Data = Data()
        private set

    /** The cosmetic to render for the local player, or `null` when disabled or empty. */
    val active: ElytraCosmetic?
        get() = data.takeIf { it.enabled }?.cosmetic?.takeUnless { it.isEmpty }

    fun load() {
        val file = path
        if (!file.exists()) return
        data = runCatching {
            val json = file.reader().use(JsonParser::parseReader)
            Data.CODEC.parse(JsonOps.INSTANCE, json).resultOrPartial { LOGGER.warn("Invalid cosmetic config: $it") }.orElseThrow()
        }.getOrElse {
            LOGGER.error("Failed to read $file, using defaults", it)
            Data()
        }
    }

    /** Replaces the saved designs only, keeping the applied cosmetic as it was saved. */
    fun updateDesigns(designs: List<Design>) = update(data.copy(designs = designs))

    fun update(new: Data) {
        data = new
        save()
    }

    private fun save() {
        val file = path
        runCatching {
            val json = Data.CODEC.encodeStart(JsonOps.INSTANCE, data).orThrow
            file.createParentDirectories()
            val temp = file.resolveSibling("${file.name}.tmp")
            temp.writer().use { GSON.toJson(json, it) }
            temp.moveTo(file, overwrite = true)
        }.onFailure { LOGGER.error("Failed to save $file", it) }
    }
}
