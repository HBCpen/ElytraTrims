package dev.kikugie.elytratrims.resource.provider

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
import net.minecraft.client.renderer.item.properties.conditional.HasComponent
import net.minecraft.client.renderer.item.properties.select.ComponentContents
import net.minecraft.client.renderer.item.properties.select.TrimMaterialProperty
import net.minecraft.client.resources.model.sprite.Material
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.equipment.trim.TrimMaterial
import java.util.function.BiConsumer

private typealias TrimMaterialKey = ResourceKey<TrimMaterial>

object ETItemModelGenerator : ETResourceProvider {
    override fun collect(lookup: ResourceManager, to: MutableMap<PackIdentifier, InputSupplier>) {
        val generators: ItemModelGenerators = LazyModelOutput(to).let { ItemModelGenerators(it, it) }
        val materials: Set<TrimMaterialKey> = ETAtlasGenerator.collectPalettedPermutations(lookup).asSequence()
            .flatMap { it.permutations.keys }.distinct()
            .mapNotNull { if (it.endsWith("_darker")) null else Identifier.tryParse(it) }
            .map { ResourceKey.create(Registries.TRIM_MATERIAL, it) }
            .toSet()
        generators.generateElytraModel(Items.ELYTRA, materials)
    }

    private fun ItemModelGenerators.generateElytraModel(item: Item, materials: Iterable<TrimMaterialKey>) {
        val model = conditional(
            Broken(),
            generateBrokenElytraVariants(item, true, materials),
            generateBrokenElytraVariants(item, false, materials),
        )
        itemModelOutput.accept(item, model)
    }

    private fun ItemModelGenerators.generateBrokenElytraVariants(item: Item, broken: Boolean, materials: Iterable<TrimMaterialKey>): ItemModel.Unbaked {
        return conditional(
            HasComponent(DataComponents.DYED_COLOR, true),
            generateOverlayElytraVariants(item, broken, true, materials),
            generateOverlayElytraVariants(item, broken, false, materials),
        )
    }

    private fun ItemModelGenerators.generateOverlayElytraVariants(item: Item, broken: Boolean, overlay: Boolean, materials: Iterable<TrimMaterialKey>): ItemModel.Unbaked {
        val base = if (overlay) generateOverlayModel(item, broken) else plainModel(item.model(isBroken = broken))
        val cases = materials.map {
            `when`(it, composite(base, generateTrimModel(item, broken, it.identifier().path)))
        }

        return select(TrimMaterialProperty(), base, cases)
    }

    private fun ItemModelGenerators.generateOverlayModel(item: Item, broken: Boolean): ItemModel.Unbaked {
        val destination = item.model(isBroken = broken, isOverlay = true)
        FLAT_ITEM.create(destination, TextureMapping.layer0(item.texture(isBroken = broken, isOverlay = true).toMaterial()), modelOutput)
        return tintedModel(destination, Dye(0))
    }

    private fun ItemModelGenerators.generateTrimModel(item: Item, broken: Boolean, material: String): ItemModel.Unbaked {
        val destination = item.model(isBroken = broken, itsMaterial = material)
        val trim = BuiltInRegistries.ITEM.getKey(item).withPath("trims/items/wings${buildSuffix(itsMaterial = material)}")

        FLAT_ITEM.create(destination, TextureMapping.layer0(trim.toMaterial()), modelOutput)
        return plainModel(destination)
    }

    private fun buildSuffix(isBroken: Boolean = false, isOverlay: Boolean = false, itsMaterial: String = ""): String = buildString {
        if (isBroken) append("_broken")
        if (isOverlay) append("_overlay")
        if (itsMaterial.isNotEmpty()) append("_trim_$itsMaterial")
    }

    private fun Item.model(isBroken: Boolean = false, isOverlay: Boolean = false, itsMaterial: String = ""): Identifier =
        ModelLocationUtils.getModelLocation(this, buildSuffix(isBroken, isOverlay, itsMaterial))

    private fun Item.texture(isBroken: Boolean = false, isOverlay: Boolean = false, itsMaterial: String = ""): Identifier =
        TextureMapping.getItemTexture(this, buildSuffix(isBroken, isOverlay, itsMaterial)).sprite

    private fun Identifier.toMaterial(): Material =
        Material(this)

    private class LazyModelOutput(val to: MutableMap<PackIdentifier, InputSupplier>) : ItemModelOutput, BiConsumer<Identifier, ModelInstance> {
        override fun accept(item: Item, generator: ItemModel.Unbaked, properties: ClientItem.Properties) {
            to[model("items", BuiltInRegistries.ITEM.getKey(item))] = jsonSupplier(ClientItem.CODEC) {
                ClientItem(generator, properties)
            }
        }

        override fun accept(id: Identifier, instance: ModelInstance) {
            to[model("models", id)] = lazySupplier { instance.get().asString() }
        }

        override fun copy(item: Item, item2: Item) {
            throw UnsupportedOperationException("Not yet implemented")
        }

        private fun model(type: String, item: Identifier): PackIdentifier =
            PackIdentifier.of(PackType.CLIENT_RESOURCES, item.withPath { "$type/$it.json" })
    }
}