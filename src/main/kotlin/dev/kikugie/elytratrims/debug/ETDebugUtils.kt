package dev.kikugie.elytratrims.debug

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import dev.kikugie.elytratrims.debug.command.register
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.coordinates.Coordinates
import net.minecraft.commands.arguments.item.ItemInput
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.equipment.trim.ArmorTrim
import net.minecraft.world.item.equipment.trim.TrimMaterials
import kotlin.jvm.optionals.getOrElse
import kotlin.math.ceil
import kotlin.math.sqrt

fun registerServerDebugCommands(
    dispatcher: CommandDispatcher<CommandSourceStack>,
    access: CommandBuildContext,
    env: Commands.CommandSelection
) = with(dispatcher) {
    register("elytratrims:spawn_variants", access) {
        argument<Coordinates>("position") { pos ->
            argument("dimension", IntegerArgumentType.integer(1)) { dim ->
                runs {
                    spawnArmorStands(source, access, pos(),  dim())
                }
            }

            runs {
                spawnArmorStands(source, access, pos())
            }
        }
    }
}

private fun spawnArmorStands(source: CommandSourceStack, access: CommandBuildContext, position: Coordinates, dimension: Int = -1) {
    val trims = access.lookupOrThrow(Registries.TRIM_PATTERN)
        .listElements().toList()
    val materials = access.lookupOrThrow(Registries.TRIM_MATERIAL).listElements().toList()
        .let { Shuffler(it) }
    val start = position.getBlockPos(source)

    val (len, wid) =
        if (dimension < 0) determineDimensions(trims.size)
        else dimension to ceil(trims.size / dimension.toFloat()).toInt()

    outer@for (z in 0..<wid) for (x in 0..<len) {
        val index = x + z * len
        val pattern = trims.getOrElse(index) { break@outer }.value().assetId
        val material = materials.next().value().assets.base.suffix

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

private class Shuffler<T>(items: Collection<T>) {
    private val shuffled = ArrayList(items).apply { shuffle() }
    private var index = 0

    fun next(): T = shuffled[index++].also {
        if (index >= shuffled.size) {
            index = 0
            shuffled.shuffle()

            if (shuffled.first() == it)
                index++
        }
    }
}