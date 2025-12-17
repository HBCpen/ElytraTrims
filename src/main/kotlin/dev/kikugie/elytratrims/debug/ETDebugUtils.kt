package dev.kikugie.elytratrims.debug

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import dev.kikugie.elytratrims.api.item.ETItemFlag
import dev.kikugie.elytratrims.debug.command.*
import dev.kikugie.elytratrims.recipe.ETSmithingRecipe
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.equipment.trim.TrimMaterial
import net.minecraft.world.item.equipment.trim.TrimPattern
import net.minecraft.world.level.block.entity.BannerPattern
import kotlin.math.ceil
import kotlin.math.sqrt

fun registerServerDebugCommands(
    dispatcher: CommandDispatcher<CommandSourceStack>,
    access: CommandBuildContext,
    env: Commands.CommandSelection
) = with(dispatcher) {
    register("elytratrims:spawn_variants", access) {
        argument("length", IntegerArgumentType.integer(1)) { dim ->
            argument<Boolean>("all") { all ->
                runs { spawnArmorStands(source, access, dim(), all()) }
            }
        }

        argument<Boolean>("all") { all ->
            runs { spawnArmorStands(source, access, -1, all()) }
        }

        runs { spawnArmorStands(source, access) }
    }

    register("elytratrims:configure_elytra", access) {

        val (trimLiteral, trimActions) = trim<CommandSourceStack>()
        val (patternLiteral, patternActions) = pattern<CommandSourceStack>()
        val (colorLiteral, colorActions) = color<CommandSourceStack>()
        val (flagLiteral, flagActions) = flag<CommandSourceStack>()

        children += listOf(trimLiteral, patternLiteral, colorLiteral, flagLiteral)
        for (list in listOf(trimActions, patternActions, colorActions, flagActions))
            for ((builder, modifier) in list)
                builder.runs { modifyItemInHand(this, modifier) }
    }

    register("elytratrims:advance_recipe", access) {

        runs { ETSmithingRecipe.advance() }
    }
}

private fun modifyItemInHand(context: CommandContext<CommandSourceStack>, modifier: ItemModifier<CommandSourceStack>) {
    var elytra = context.source.playerOrException.getItemInHand(InteractionHand.MAIN_HAND)
    if (elytra.isEmpty) {
        elytra = ItemStack(Items.ELYTRA).apply { modifier.apply(context, this) }
        context.source.playerOrException.setItemInHand(InteractionHand.MAIN_HAND, elytra)
    }
    modifier.apply(context, elytra)
}

private fun spawnArmorStands(source: CommandSourceStack, access: CommandBuildContext, dimension: Int = -1, all: Boolean = false) {
    val start = source.position.run { BlockPos(x.toInt(), y.toInt(), z.toInt()) }
    val entries = buildList<ArmorStandSummoner> {
        this += ArmorStandSummoner.Trimmed.create(access)
        if (!all) return@buildList

        this += ArmorStandSummoner.Pattern.create(access)
        this += ArmorStandSummoner.Dyed()
        this += ArmorStandSummoner.Flag(ETItemFlag.BAD_APPLE)
        this += ArmorStandSummoner.Flag(ETItemFlag.GATEWAY)
    }

    val (len, wid) =
        if (dimension < 0) determineDimensions(entries.size)
        else dimension to ceil(entries.size / dimension.toFloat()).toInt()

    outer@ for (z in 0..<wid) for (x in 0..<len) {
        val entry = entries.getOrElse(x + z * len) { break@outer }
        entry.summon(source, start.x + x + .5, start.y.toDouble(), start.z + z + .5)
    }
}

private fun interface ArmorStandSummoner {
    fun summon(source: CommandSourceStack, x: Double, y: Double, z: Double)

    class Trimmed(val pattern: Holder<TrimPattern>, val material: Holder<TrimMaterial>) : ArmorStandSummoner {
        override fun summon(source: CommandSourceStack, x: Double, y: Double, z: Double) {
            val pattern = pattern.value().assetId()
            val material = material.value().assets().base().suffix()
            val command =
                "summon minecraft:armor_stand $x $y $z {equipment: {chest: {components: {\"minecraft:trim\": {pattern: \"$pattern\", material: \"$material\"}}, count: 1, id: \"minecraft:elytra\"}}}"
            source.dispatcher().execute(command, source)
        }

        companion object {
            fun create(access: HolderLookup.Provider): List<Trimmed> {
                val trims = access.lookupOrThrow(Registries.TRIM_PATTERN).listElements().toList().shuffled()
                val materials = access.lookupOrThrow(Registries.TRIM_MATERIAL).listElements().toList()
                return trims.map { Trimmed(it, materials.random()) }
            }
        }
    }

    class Pattern(val pattern: Holder<BannerPattern>, val color: DyeColor, val hd: Boolean) : ArmorStandSummoner {
        override fun summon(source: CommandSourceStack, x: Double, y: Double, z: Double) {
            val pattern = pattern.value().assetId()
            val color = color.getName()
            val flag = if (hd) ", \"minecraft:custom_data\": {\"elytratrims:banner\": true}" else ""
            val command =
                "summon minecraft:armor_stand $x $y $z {equipment: {chest: {components: {\"minecraft:banner_patterns\": [{pattern: \"$pattern\", color: \"$color\"}]$flag}, count: 1, id: \"minecraft:elytra\"}}}"
            source.dispatcher().execute(command, source)
        }

        companion object {
            fun create(access: HolderLookup.Provider): List<Pattern> {
                val patterns = access.lookupOrThrow(Registries.BANNER_PATTERN).listElements().toList()
                return patterns.shuffled().map { Pattern(it, DyeColor.entries.random(), true) } + patterns.shuffled()
                    .map { Pattern(it, DyeColor.entries.random(), false) }
            }
        }
    }

    class Dyed(val color: DyeColor = DyeColor.entries.random()) : ArmorStandSummoner {
        override fun summon(source: CommandSourceStack, x: Double, y: Double, z: Double) {
            val command =
                "summon minecraft:armor_stand $x $y $z {equipment: {chest: {components: {\"minecraft:dyed_color\": ${color.textureDiffuseColor}}, count: 1, id: \"minecraft:elytra\"}}}"
            source.dispatcher().execute(command, source)
        }
    }

    class Flag(val flag: ETItemFlag) : ArmorStandSummoner {
        override fun summon(source: CommandSourceStack, x: Double, y: Double, z: Double) {
            val command =
                "summon minecraft:armor_stand $x $y $z {equipment: {chest: {components: {\"minecraft:custom_data\": {\"${flag.key}\": true}}, count: 1, id: \"minecraft:elytra\"}}}"
            source.dispatcher().execute(command, source)
        }
    }
}

private fun determineDimensions(elements: Int): Pair<Int, Int> {
    if (elements == 1) return 1 to 1
    val sqrt = sqrt(elements.toDouble()).toInt()
    for (i in sqrt downTo 1) if (elements % i == 0)
        return elements / i to i
    return elements to 1
}