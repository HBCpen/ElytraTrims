package dev.kikugie.elytratrims.debug.command

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import dev.kikugie.elytratrims.item.ETItemFlag
import dev.kikugie.elytratrims.item.ColorAccess.Companion.color
import dev.kikugie.elytratrims.item.PatternsAccess.Companion.patterns
import dev.kikugie.elytratrims.item.TrimAccess.Companion.trim
import dev.kikugie.elytratrims.resource.image.toColor4i
import dev.kikugie.elytratrims.text
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.arguments.ResourceKeyArgument
import net.minecraft.commands.arguments.IdentifierArgument
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.equipment.trim.ArmorTrim
import net.minecraft.world.item.equipment.trim.TrimMaterial
import net.minecraft.world.item.equipment.trim.TrimPattern
import net.minecraft.world.level.block.entity.BannerPattern
import net.minecraft.world.level.block.entity.BannerPatternLayers
import kotlin.text.uppercase

val ET_FLAGS = listOf(ETItemFlag.GLOW, ETItemFlag.BANNER, ETItemFlag.GATEWAY, ETItemFlag.BAD_APPLE, ETItemFlag.DEBUG)
    .associateBy(ETItemFlag::key)

val NO_SUCH_FLAG = SimpleCommandExceptionType("Invalid item flag".text())

typealias ItemModifierAction<Source> = Pair<CommandBuilder<Source, *, *>, ItemModifier<Source>>

fun <Source> trim(): Pair<LiteralCommandBuilder<Source>, List<ItemModifierAction<Source>>>
where Source : SharedSuggestionProvider {
    val literal = LiteralCommandBuilder<Source>("trim")
    val actions = mutableListOf<ItemModifierAction<Source>>()

    literal.argument("pattern", ResourceKeyArgument.key(Registries.TRIM_PATTERN)) { pattern ->
        argument("material", ResourceKeyArgument.key(Registries.TRIM_MATERIAL)) { material ->
            actions += this to ItemModifier.Trim(pattern, material)
        }

        val material: ArgumentResolver<Source, ResourceKey<TrimMaterial>> = {
            source.registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL).listElements().toList()
                .random().key()
        }
        actions += this to ItemModifier.Trim(pattern, material)
    }

    return literal to actions
}

fun <Source> pattern() : Pair<LiteralCommandBuilder<Source>, List<ItemModifierAction<Source>>>
where Source : SharedSuggestionProvider {
    val literal = LiteralCommandBuilder<Source>("pattern")
    val actions = mutableListOf<ItemModifierAction<Source>>()

    literal.argument("pattern", ResourceKeyArgument.key(Registries.BANNER_PATTERN)) { pattern ->
        argument("color", StringArgumentType.word()) { color ->
            suggestList { DyeColor.entries.map(DyeColor::getName) }
            val dye: ArgumentResolver<Source, DyeColor> = {
                DyeColor.valueOf(color().uppercase())
            }
            actions += this to ItemModifier.Pattern(pattern, dye)
        }

        val dye: ArgumentResolver<Source, DyeColor> = {
            DyeColor.entries.random()
        }
        actions += this to ItemModifier.Pattern(pattern, dye)
    }
    return literal to actions
}

fun <Source> color() : Pair<LiteralCommandBuilder<Source>, List<ItemModifierAction<Source>>>
where Source : SharedSuggestionProvider {
    val literal = LiteralCommandBuilder<Source>("color")
    val actions = mutableListOf<ItemModifierAction<Source>>()

    literal.argument("color", StringArgumentType.word()) {
        suggestList { DyeColor.entries.map(DyeColor::getName) }

        val dye: ArgumentResolver<Source, DyeColor> = {
            DyeColor.valueOf(it().uppercase())
        }
        actions += this to ItemModifier.Color(dye)
    }
    return literal to actions
}

fun <Source> flag() : Pair<LiteralCommandBuilder<Source>, List<ItemModifierAction<Source>>>
    where Source : SharedSuggestionProvider {
    val literal = LiteralCommandBuilder<Source>("flag")
    val actions = mutableListOf<ItemModifierAction<Source>>()

    literal.argument("flag", IdentifierArgument.id()) { id ->
        suggestList { ET_FLAGS.keys }
        argument<Boolean>("value") { value ->
            val flag: ArgumentResolver<Source, ETItemFlag> = {
                ET_FLAGS[id()] ?: throw NO_SUCH_FLAG.create()
            }
            actions += this to ItemModifier.Flag(flag, value)
        }
        val flag: ArgumentResolver<Source, ETItemFlag> = {
            ET_FLAGS[id()] ?: throw NO_SUCH_FLAG.create()
        }
        val value: ArgumentResolver<Source, Boolean> = { true }
        actions += this to ItemModifier.Flag(flag, value)
    }
    return literal to actions
}

interface ItemModifier<Source> where Source : SharedSuggestionProvider {
    fun apply(context:CommandContext<Source>, stack: ItemStack)

    class Trim<Source>(val pattern: ArgumentResolver<Source, ResourceKey<TrimPattern>>, val material: ArgumentResolver<Source, ResourceKey<TrimMaterial>>) : ItemModifier<Source> where Source : SharedSuggestionProvider {
        override fun apply(context:CommandContext<Source>, stack: ItemStack) {
            val pattern = context.source.registryAccess().lookupOrThrow(Registries.TRIM_PATTERN).getOrThrow(context.pattern())
            val material = context.source.registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL).getOrThrow(context.material())
            stack.trim set ArmorTrim(material, pattern)
        }
    }

    class Pattern<Source>(val pattern: ArgumentResolver<Source, ResourceKey<BannerPattern>>, val color: ArgumentResolver<Source, DyeColor>) : ItemModifier<Source> where Source : SharedSuggestionProvider  {
        override fun apply(context:CommandContext<Source>, stack: ItemStack) {
            val pattern = context.source.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN).getOrThrow(context.pattern())
            val layer = BannerPatternLayers.Layer(pattern, context.color())

            val patterns = stack.patterns
            patterns set BannerPatternLayers(patterns.get()?.layers.orEmpty() + layer)
        }
    }

    class Color<Source>(val color: ArgumentResolver<Source, DyeColor>) : ItemModifier<Source> where Source : SharedSuggestionProvider {
        override fun apply(context:CommandContext<Source>, stack: ItemStack) {
            stack.color set context.color().textureDiffuseColor.toColor4i()
        }
    }

    class Flag<Source>(val flag: ArgumentResolver<Source, ETItemFlag>, val value: ArgumentResolver<Source, Boolean>) : ItemModifier<Source> where Source : SharedSuggestionProvider {
        override fun apply(context:CommandContext<Source>, stack: ItemStack) {
            context.flag()[stack] = context.value()
        }
    }
}