package dev.kikugie.elytratrims.debug

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import dev.kikugie.elytratrims.debug.command.*
import dev.kikugie.elytratrims.recipe.ETSmithingRecipe
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.coordinates.Coordinates
import net.minecraft.core.registries.Registries
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.math.ceil
import kotlin.math.sqrt

fun registerServerDebugCommands(
    dispatcher: CommandDispatcher<CommandSourceStack>,
    access: CommandBuildContext,
    env: Commands.CommandSelection
) = with(dispatcher) {
    register("elytratrims:spawn_variants", access) {
        requiresPermissionLevel(PermissionLevel.OWNER)

        argument<Coordinates>("position") { pos ->
            argument("dimension", IntegerArgumentType.integer(1)) { dim ->
                runs { spawnArmorStands(source, access, pos(), dim()) }
            }

            runs { spawnArmorStands(source, access, pos()) }
        }
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

private fun spawnArmorStands(source: CommandSourceStack, access: CommandBuildContext, position: Coordinates, dimension: Int = -1) {
    val trims = access.lookupOrThrow(Registries.TRIM_PATTERN)
        .listElements().toList().shuffled()
    val materials = access.lookupOrThrow(Registries.TRIM_MATERIAL).listElements().toList()
    val start = position.getBlockPos(source)

    val (len, wid) =
        if (dimension < 0) determineDimensions(trims.size)
        else dimension to ceil(trims.size / dimension.toFloat()).toInt()

    outer@for (z in 0..<wid) for (x in 0..<len) {
        val index = x + z * len
        val pattern = trims.getOrElse(index) { break@outer }.value().assetId
        val material = materials.random().value().assets.base.suffix

        val command = "summon minecraft:armor_stand ${start.x + x +.5} ${start.y} ${start.z + z +.5} {equipment: {chest: {components: {\"minecraft:trim\": {material: \"$material\", pattern: \"$pattern\"}}, count: 1, id: \"minecraft:elytra\"}}}"
        source.dispatcher().execute(command, source)
    }
}

private fun determineDimensions(elements: Int): Pair<Int, Int> {
    if (elements == 1) return 1 to 1
    val sqrt = sqrt(elements.toDouble()).toInt()
    for (i in sqrt downTo 1) if (elements % i == 0)
        return elements / i to i
    return elements to 1
}