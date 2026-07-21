package dev.kikugie.elytratrims.mixin;

import dev.kikugie.elytratrims.item.ItemFlagTooltipProvider;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Shadow
    public abstract DataComponentMap getComponents();

    //? if fabric {
    @Inject(method = "addDetailsToTooltip", at = @At(value = "INVOKE", ordinal = 19, target = "Lnet/minecraft/world/item/ItemStack;addToTooltip(Lnet/minecraft/core/component/DataComponentType;Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/item/component/TooltipDisplay;Ljava/util/function/Consumer;Lnet/minecraft/world/item/TooltipFlag;)V"))
    //?} else
    //@Inject(method = "addDetailsToTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;appendHoverText(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/item/component/TooltipDisplay;Ljava/util/function/Consumer;Lnet/minecraft/world/item/TooltipFlag;)V"))
    private void injectETTooltips(Item.TooltipContext context, TooltipDisplay display, @Nullable Player player, TooltipFlag flag, Consumer<Component> adder, CallbackInfo ci) {
        ItemFlagTooltipProvider.GLOW.addToTooltip(context, adder, flag, getComponents());
        ItemFlagTooltipProvider.COSMIC.addToTooltip(context, adder, flag, getComponents());
        ItemFlagTooltipProvider.BAD_APPLE.addToTooltip(context, adder, flag, getComponents());
        ItemFlagTooltipProvider.BANNER.addToTooltip(context, adder, flag, getComponents());
    }
}
