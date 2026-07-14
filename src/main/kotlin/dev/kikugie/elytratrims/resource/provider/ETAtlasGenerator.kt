package dev.kikugie.elytratrims.resource.provider

import com.google.gson.JsonParser
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.serialization.Dynamic
import com.mojang.serialization.JsonOps
import dev.kikugie.elytratrims.elytratrims
import dev.kikugie.elytratrims.resource.pack.InputSupplier
import dev.kikugie.elytratrims.resource.pack.PackIdentifier
import dev.kikugie.elytratrims.resource.pack.readJsonWith
import dev.kikugie.elytratrims.resource.reload.ElytraTrimsAtlas
import dev.kikugie.elytratrims.vanilla
import net.minecraft.client.renderer.texture.atlas.SpriteSource
import net.minecraft.client.renderer.texture.atlas.SpriteSources
import net.minecraft.client.renderer.texture.atlas.sources.DirectoryLister
import net.minecraft.client.renderer.texture.atlas.sources.PalettedPermutations
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.ResourceManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Predicate

private typealias TrimTextures = MutableSet<Identifier>
private typealias TrimPalette = Identifier
private typealias TrimMaterials = MutableMap<String, Identifier>

object ETAtlasGenerator : ETResourceProvider {
    private val LOGGER: Logger = LoggerFactory.getLogger("ET Atlas Generator")

    override fun collect(lookup: ResourceManager, to: MutableMap<PackIdentifier, InputSupplier>) {
        val sources by lazy { collectPalettedPermutations(lookup) }

        to[atlas(ElytraTrimsAtlas.ATLAS_ID)] = jsonSupplier(SpriteSources.FILE_CODEC) {
            buildList {
                this += SingleFile(elytratrims("feature/bad_apple"))
                this += "entity/wings/banner".let { DirectoryLister(it, "$it/") }
                this += "entity/wings/shield".let { DirectoryLister(it, "$it/") }
                //? if <26.3
                //this += sources
            }
        }

        to[atlas(vanilla("items"))] = jsonSupplier(SpriteSources.FILE_CODEC) {
            val textures = listOf(vanilla("trims/items/wings_trim"), vanilla("trims/items/wings_broken_trim"))
            sources.map { PalettedPermutations(textures, it.paletteKey, it.permutations) }
        }
    }

    fun collectPatterns(lookup: ResourceManager, useBanners: Boolean): Collection<Identifier> {
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

    fun collectPalettedPermutations(lookup: ResourceManager): Collection<PalettedPermutations> {
        val builder = TrimPermutationsStorage()
        val sources = lookup.listResourceStacks("atlases") { it.isFile("armor_trims.json") || it.isFile("items.json") }
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

    private fun atlas(id: Identifier): PackIdentifier =
        PackIdentifier.of(PackType.CLIENT_RESOURCES, id.namespace, "atlases/${id.path}.json")

    private fun Identifier.isFile(name: String): Boolean =
        path.substringAfterLast('/') == name

    private class ProxyOutput(val delegate: MutableSet<Identifier>) : SpriteSource.Output {
        override fun add(identifier: Identifier, discardableLoader: SpriteSource.DiscardableLoader) {
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