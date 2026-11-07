package dev.kikugie.elytratrims.cosmetic

import dev.kikugie.elytratrims.Text
import dev.kikugie.elytratrims.vanilla
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.world.item.DyeColor
import kotlin.jvm.optionals.getOrNull

/**
 * Options offered by the cosmetic editor.
 */
object CosmeticCatalog {
    private const val WING_TRIMS = "textures/trims/entity/wings"

    /** Vanilla trim materials, available even when the server registry is unknown. */
    private val VANILLA_MATERIALS: List<Identifier> = listOf(
        "quartz", "iron", "netherite", "redstone", "copper", "gold", "emerald", "diamond", "lapis", "amethyst", "resin"
    ).map(::vanilla)

    /** Every trim pattern that has a wings texture, regardless of whether the server knows the pattern. */
    fun trimPatterns(resources: ResourceManager): List<Identifier> = resources
        .listResources(WING_TRIMS) { it.path.endsWith(".png") }.keys
        .map { id -> id.withPath { it.removePrefix("$WING_TRIMS/").removeSuffix(".png") } }
        .distinct()
        .sortedWith(IDENTIFIER_ORDER)

    fun trimMaterials(registries: RegistryAccess?): List<Identifier> {
        val registered = registries?.lookup(Registries.TRIM_MATERIAL)?.getOrNull()?.keySet().orEmpty()
        return (VANILLA_MATERIALS + registered).distinct().sortedWith(IDENTIFIER_ORDER)
    }

    fun bannerPatterns(registries: RegistryAccess?): List<Identifier> =
        registries?.lookup(Registries.BANNER_PATTERN)?.getOrNull()?.keySet().orEmpty()
            .sortedWith(IDENTIFIER_ORDER)

    fun trimPatternName(id: Identifier, registries: RegistryAccess?): Text =
        CosmeticStacks.pattern(id, registries).value().description()

    fun trimMaterialName(id: Identifier, registries: RegistryAccess?): Text =
        CosmeticStacks.material(id, registries).value().description()

    fun bannerPatternName(id: Identifier, color: DyeColor, registries: RegistryAccess?): Text =
        Text.translatable("${CosmeticStacks.bannerPattern(id, registries).value().translationKey()}.${color.getName()}")

    fun dyeName(color: DyeColor): Text = Text.translatable("color.minecraft.${color.getName()}")

    /** Vanilla first, then everything else alphabetically. */
    private val IDENTIFIER_ORDER: Comparator<Identifier> =
        compareBy<Identifier> { it.namespace != Identifier.DEFAULT_NAMESPACE }.thenBy { it.namespace }.thenBy { it.path }
}
