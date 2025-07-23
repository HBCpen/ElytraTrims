package dev.kikugie.elytratrims.resource.provider

import com.google.gson.JsonElement
import com.mojang.serialization.JsonOps
import dev.kikugie.elytratrims.Identifier
import dev.kikugie.elytratrims.resource.pack.InputSupplier
import dev.kikugie.elytratrims.resource.pack.PackIdentifier
import net.minecraft.client.color.item.Dye
import net.minecraft.client.data.models.ItemModelGenerators
import net.minecraft.client.data.models.ItemModelOutput
import net.minecraft.client.data.models.model.ItemModelUtils.*
import net.minecraft.client.data.models.model.ModelInstance
import net.minecraft.client.data.models.model.ModelLocationUtils
import net.minecraft.client.data.models.model.ModelTemplates.FLAT_ITEM
import net.minecraft.client.data.models.model.TextureMapping
import net.minecraft.client.renderer.item.ClientItem
import net.minecraft.client.renderer.item.ItemModel
import net.minecraft.client.renderer.item.properties.conditional.Broken
import net.minecraft.client.renderer.item.properties.select.TrimMaterialProperty
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.equipment.trim.TrimMaterial
import java.util.function.BiConsumer

private typealias TrimMaterialKey = ResourceKey<TrimMaterial>

class ETItemModelGenerator(val lookup: ResourceManager) : ETResourceProvider<JsonElement>() {
    private class ModelProxy(val models: MutableMap<PackIdentifier, JsonElement>) : ItemModelOutput, BiConsumer<Identifier, ModelInstance> {
        override fun accept(item: Item, model: ItemModel.Unbaked) {
            val id = BuiltInRegistries.ITEM.getKey(item)
            val path = PackIdentifier.of(PackType.CLIENT_RESOURCES, id.withPath { "items/$it.json" })
            models[path] = ClientItem.CODEC.encodeStart(JsonOps.INSTANCE, ClientItem(model, ClientItem.Properties.DEFAULT)).orThrow
        }

        override fun accept(identifier: Identifier, model: ModelInstance) {
            val path = PackIdentifier.of(PackType.CLIENT_RESOURCES, identifier.withPath { "models/$it.json" })
            models[path] = model.get()
        }

        override fun copy(item: Item, item2: Item) {
            throw UnsupportedOperationException("Not yet implemented")
        }
    }

    override fun convert(value: JsonElement): InputSupplier = GSON.toJson(value)
        .let(::StringSupplier)

    override fun generate(): Map<PackIdentifier, JsonElement> = buildMap {
        val generators: ItemModelGenerators = ModelProxy(this).let { ItemModelGenerators(it, it) }
        val materials: Set<TrimMaterialKey> = ETAtlasGenerator(lookup).collectPalettedPermutations().asSequence()
            .flatMap { it.permutations.keys }.distinct()
            .mapNotNull(Identifier::parse)
            .map { ResourceKey.create(Registries.TRIM_MATERIAL, it) }
            .toSet()
        // for (it in BuiltInRegistries.ITEM.get(ETTags.ELYTRA_DECORATABLE).getOrElse { return@buildMap })
        //     generators.generateElytraModel(it.value(), materials)
        // FIXME: tags are not available at this point, so maybe add an API call to register custom elytras
        generators.generateElytraModel(Items.ELYTRA, materials)
    }

    private fun buildSuffix(broken: Boolean = false, overlay: Boolean = false, material: String = ""): String = buildString {
        if (overlay) append("_overlay")
        if (broken) append("_broken")
        if (material.isNotEmpty()) append("_trim_$material")
    }

    private fun Item.model(broken: Boolean = false, overlay: Boolean = false, material: String = ""): Identifier =
        ModelLocationUtils.getModelLocation(this, buildSuffix(broken, overlay, material))

    private fun Item.texture(broken: Boolean = false, overlay: Boolean = false, material: String = ""): Identifier =
        TextureMapping.getItemTexture(this, buildSuffix(broken, overlay, material))

    private fun ItemModelGenerators.generateElytraModel(item: Item, materials: Iterable<TrimMaterialKey>) {
        val model = select(TrimMaterialProperty(), generateFallbackModel(item), generateElytraVariants(item, materials))
        itemModelOutput.accept(item, model)
    }

    private fun ItemModelGenerators.generateElytraVariants(item: Item, materials: Iterable<TrimMaterialKey>) = buildList {
        val regularBase = plainModel(item.model(false))
        val regularOverlay = tintedModel(item.model(broken = false, overlay = true), Dye(0))

        val brokenBase = plainModel(item.model(true))
        val brokenOverlay = tintedModel(item.model(broken = true, overlay = true), Dye(0))

        for (it in materials) {
            val material = it.location().path
            val regular = composite(regularBase, regularOverlay, generateTrimModel(item, false, material))
            val broken = composite(brokenBase, brokenOverlay, generateTrimModel(item, true, material))
            val selector = conditional(Broken(), broken, regular)
            this += `when`(it, selector)
        }
    }

    private fun ItemModelGenerators.generateFallbackModel(item: Item): ItemModel.Unbaked {
        val regular = composite(plainModel(item.model(false)), generateOverlayModel(item, false))
        val broken = composite(plainModel(item.model(true)), generateOverlayModel(item, true))
        return conditional(Broken(), broken, regular)
    }

    private fun ItemModelGenerators.generateOverlayModel(item: Item, broken: Boolean): ItemModel.Unbaked {
        val destination = item.model(broken, true)
        val overlay = item.texture(broken, true)

        FLAT_ITEM.create(destination, TextureMapping.layer0(overlay), modelOutput)
        return tintedModel(destination, Dye(0))
    }

    private fun ItemModelGenerators.generateTrimModel(item: Item, broken: Boolean, material: String): ItemModel.Unbaked {
        val destination = item.model(broken, false, material)
        val trim = BuiltInRegistries.ITEM.getKey(item).withPath("trims/items/wings${buildSuffix(material = material)}")

        FLAT_ITEM.create(destination, TextureMapping.layer0(trim), modelOutput)
        return plainModel(destination)
    }
}