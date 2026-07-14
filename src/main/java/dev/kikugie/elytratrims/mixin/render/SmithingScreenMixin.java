package dev.kikugie.elytratrims.mixin.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.kikugie.elytratrims.item.ETItemFlag;
import dev.kikugie.elytratrims.render.gui.PreviewElytraAnimationState;
import dev.kikugie.elytratrims.render.gui.SlotDoohickeyManager;
import dev.kikugie.elytratrims.resource.ETTags;
import dev.kikugie.fletching_table.annotation.MixinEnvironment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.CyclingSlotBackground;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@MixinEnvironment(type = MixinEnvironment.Env.CLIENT)
@Mixin(SmithingScreen.class)
public abstract class SmithingScreenMixin extends ItemCombinerScreen<SmithingMenu> {
    @Shadow
    @Final
    private ArmorStandRenderState armorStandPreview;
    @Shadow
    @Final
    private static int ARMOR_STAND_Y_ROT;

    @Shadow
    @Final
    private CyclingSlotBackground templateIcon;
    @Shadow
    @Final
    private CyclingSlotBackground baseIcon;
    @Shadow
    @Final
    private CyclingSlotBackground additionalIcon;

    @Unique
    private final PreviewElytraAnimationState elytratrims$renderData = new PreviewElytraAnimationState();

    @Unique
    private SlotDoohickeyManager elytratrims$slotDoohickeyManager = null;

    private SmithingScreenMixin(SmithingMenu menu, Inventory inventory, Component title, Identifier menuResource) {
        super(menu, inventory, title, menuResource);
    }

    @ModifyExpressionValue(method = "updateArmorStandPreview", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;copy()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack rotateIfHasElytra(ItemStack stack) {
        elytratrims$renderData.update(stack.is(ETTags.ELYTRA_DECORATABLE));
        ETItemFlag.DEBUG.set(stack, true);
        return stack;
    }

    //~ if >26 renderBg -> extractBackground
    @Inject(method = "extractBackground", at = @At("HEAD"))
    //~ if >26 'float partialTick, int mouseX, int mouseY' -> 'int mouseX, int mouseY, float partialTick'
    private void rotateArmorStand(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        elytratrims$renderData.tick(partialTick);
        armorStandPreview.bodyRot = ARMOR_STAND_Y_ROT + elytratrims$renderData.getBodyRot();
        armorStandPreview.elytraRotX = elytratrims$renderData.getElytraXRot();
        armorStandPreview.elytraRotZ = elytratrims$renderData.getElytraZRot();
    }

    @Inject(method = "containerTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/SmithingScreen;getTemplateItem()Ljava/util/Optional;"), cancellable = true)
    private void useDoohickeyManagerIfModifyingElytra(CallbackInfo ci) {
        var base = getMenu().getSlot(1).getItem();
        if (base.is(ETTags.ELYTRA_DECORATABLE)) {
            getOrCreateDoohickeyManager().update();
            ci.cancel();
        }
    }

    @Unique
    private SlotDoohickeyManager getOrCreateDoohickeyManager() {
        if (elytratrims$slotDoohickeyManager == null) elytratrims$slotDoohickeyManager = new SlotDoohickeyManager(
            getMenu(), templateIcon, baseIcon, additionalIcon
        );
        return elytratrims$slotDoohickeyManager;
    }
}
