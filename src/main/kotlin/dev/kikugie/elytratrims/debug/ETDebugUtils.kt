package dev.kikugie.elytratrims.debug

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import dev.kikugie.elytratrims.debug.command.PermissionLevel
import dev.kikugie.elytratrims.debug.command.register
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.ResourceKeyArgument
import net.minecraft.commands.arguments.coordinates.Coordinates
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.equipment.trim.TrimMaterial
import net.minecraft.world.item.equipment.trim.TrimPattern
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
                runs { spawnArmorStands(source, access, pos(),  dim()) }
            }

            runs { spawnArmorStands(source, access, pos()) }
        }
    }

    register("elytratrims:give_elytra", access) {
        requiresPermissionLevel(PermissionLevel.OWNER)

        argument("pattern", ResourceKeyArgument.key(Registries.TRIM_PATTERN)) { pattern ->
            argument("material", ResourceKeyArgument.key(Registries.TRIM_MATERIAL)) { material ->
                runs { giveTrimmedElytra(source, access, pattern(), material()) }
            }

            runs { giveTrimmedElytra(source, access, pattern()) }
        }
    }
}

private fun giveTrimmedElytra(source: CommandSourceStack, access: CommandBuildContext, pattern: ResourceKey<TrimPattern>, material: ResourceKey<TrimMaterial>? = null) {
    val lookup = access.lookupOrThrow(Registries.TRIM_MATERIAL)
    val material = material?.let { lookup.getOrThrow(it) }
        ?: lookup.listElements().toList().random()

    val command = "give @s minecraft:elytra[trim={material:\"${material.value().assets.base.suffix}\",pattern:\"${pattern.location()}\"}]"
    source.dispatcher().execute(command, source)
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