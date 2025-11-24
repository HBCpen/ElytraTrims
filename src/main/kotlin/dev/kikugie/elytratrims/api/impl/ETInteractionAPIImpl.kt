package dev.kikugie.elytratrims.api.impl

import dev.kikugie.elytratrims.Identifier
import dev.kikugie.elytratrims.api.interaction.ETCauldronInteraction

object ETInteractionAPIImpl {
    private val cauldronInteractions: MutableMap<Identifier, ETCauldronInteraction> = mutableMapOf()
    val interactions: Collection<ETCauldronInteraction>
        get() = cauldronInteractions.values

    @JvmStatic fun registerCauldronInteraction(identifier: Identifier, interaction: ETCauldronInteraction) =
        cauldronInteractions.compute(identifier) { _, it ->
            require(it == null) { "Cauldron interaction with ID $identifier is already registered" }
            interaction
        }
}