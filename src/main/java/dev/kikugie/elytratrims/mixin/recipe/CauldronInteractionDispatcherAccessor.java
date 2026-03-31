package dev.kikugie.elytratrims.mixin.recipe;

import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CauldronInteraction.Dispatcher.class)
public interface CauldronInteractionDispatcherAccessor {
    @Invoker("put")
    void elytratrims$put(final TagKey<Item> tag, final CauldronInteraction interaction);
}
