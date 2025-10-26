package dev.kikugie.elytratrims.resource.provider

import com.google.gson.JsonParser
import com.mojang.serialization.Dynamic
import com.mojang.serialization.JsonOps
import dev.kikugie.elytratrims.Identifier
import dev.kikugie.elytratrims.resource.pack.InputSupplier
import dev.kikugie.elytratrims.resource.pack.PackIdentifier
import dev.kikugie.elytratrims.resource.pack.readJsonWith
import dev.kikugie.elytratrims.resource.reload.ElytraTrimsAtlas
import dev.kikugie.elytratrims.vanilla
import net.minecraft.client.renderer.texture.atlas.SpriteSource
import net.minecraft.client.renderer.texture.atlas.SpriteSources
import net.minecraft.client.renderer.texture.atlas.sources.DirectoryLister
import net.minecraft.client.renderer.texture.atlas.sources.PalettedPermutations
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.ResourceManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Predicate

private typealias TrimTextures = MutableSet<Identifier>
private typealias TrimPalette = Identifier
private typealias TrimMaterials = MutableMap<String, Identifier>

class ETAtlasGenerator(val lookup: ResourceManager) : ETResourceProvider<List<SpriteSource>>() {
    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger("ET Atlas Generator")
    }

    override fun generate(): Map<PackIdentifier, List<SpriteSource>> = buildMap {
        val sources = collectPalettedPermutations()

        atlas(ElytraTrimsAtlas.ATLAS_ID) {
            this += "entity/wings/banner".let { DirectoryLister(it, "$it/") }
            this += "entity/wings/shield".let { DirectoryLister(it, "$it/") }
            this += sources
        }
        atlas(vanilla("blocks")) {
            val textures = listOf("trims/items/wings_trim", "trims/items/wings_broken_trim").map(::vanilla)
            this += sources.map {
                PalettedPermutations(textures, it.paletteKey, it.permutations)
            }
        }
    }

    override fun convert(value: List<SpriteSource>): InputSupplier =
        toJsonSupplier(SpriteSources.FILE_CODEC, value)

    fun collectPatterns(useBanners: Boolean): Collection<Identifier> {
        val type = if (useBanners) "banner" else "shield"
        val atlases = lookup.listResourceStacks("atlases") { it.path.endsWith("${type}_patterns.json") }
        val proxy = ProxyOutput(mutableSetOf())
        for (res in atlases.values.asSequence().flatten()) res.runCatching {
            openAsReader()
                .use { Dynamic(JsonOps.INSTANCE, JsonParser.parseReader(it)) }
                .let { SpriteSources.FILE_CODEC.parse(it).orThrow }
                .forEach { it.run(lookup, proxy) }
        }.onFailure {
            LOGGER.error("Failed to read atlas source", it)
        }
        return proxy.delegate
    }

    fun collectPalettedPermutations(): Collection<PalettedPermutations> {
        val builder = TrimPermutationsStorage()
        val sources = lookup.listResourceStacks("atlases") { it.path.endsWith("armor_trims.json") }
            .values.asSequence().flatten()

        for (res in sources) try {
            val perms = res.readJsonWith(SpriteSources.FILE_CODEC)
                .filterIsInstance<PalettedPermutations>()
            for (it in perms)
                builder += it
        } catch (e: Exception) {
            LOGGER.error("Failed to read an atlas source", e)
        }

        val textures = lookup.listResources("textures/trims/entity/wings") { it.path.endsWith(".png") }.keys
            .map { id -> id.withPath { it.removePrefix("textures/").removeSuffix(".png") } }
        builder += textures

        return builder.build()
    }

    private inline fun MutableMap<PackIdentifier, List<SpriteSource>>.atlas(id: Identifier, action: MutableList<SpriteSource>.() -> Unit) {
        val path = PackIdentifier.of(PackType.CLIENT_RESOURCES, id.namespace, "atlases/${id.path}.json")
        this[path] = mutableListOf<SpriteSource>().apply(action)
    }

    private class ProxyOutput(val delegate: MutableSet<Identifier>) : SpriteSource.Output {
        override fun add(identifier: Identifier, ignored: SpriteSource.SpriteSupplier) {
            delegate += identifier
        }

        override fun removeAll(predicate: Predicate<Identifier>) {
            delegate.removeIf(predicate)
        }
    }

    private data class TrimPermutationsBuilder(val palette: TrimPalette, val textures: TrimTextures, val materials: TrimMaterials) {
        fun build(): PalettedPermutations = PalettedPermutations(textures.toList(), palette, materials)
    }

    @JvmInline
    private value class TrimPermutationsStorage(private val builders: MutableMap<TrimPalette, TrimPermutationsBuilder> = mutableMapOf()) {
        fun build() = builders.values.map(TrimPermutationsBuilder::build)

        operator fun plusAssign(perm: PalettedPermutations) = merge(perm.paletteKey, perm.textures, perm.permutations)
        operator fun plusAssign(textures: Collection<Identifier>) = builders.forEach { (_, it) ->
            it.textures += textures.remapTextures()
        }

        private fun merge(palette: TrimPalette, textures: Collection<Identifier>, materials: Map<String, Identifier>) {
            val candidate = builders[palette] ?: return run {
                builders[palette] = TrimPermutationsBuilder(palette, textures.remapTextures(), materials.remapMaterials())
            }

            candidate.textures += textures.remapTextures()
            candidate.materials += materials.remapMaterials()
        }

        private fun Collection<Identifier>.remapTextures(): TrimTextures = mutableSetOf<Identifier>().apply {
            for (tex in this@remapTextures) when {
                "humanoid" in tex.path && "leggings" !in tex.path -> this += tex.withPath { it.replace("humanoid", "wings") }
                "wings" in tex.path -> this += tex
            }
        }

        private fun Map<String, Identifier>.remapMaterials(): TrimMaterials = mutableMapOf<String, Identifier>().apply {
            for ((id, palette) in this@remapMaterials)
                if (!id.endsWith("_darker")) this[id] = palette
        }
    }
}