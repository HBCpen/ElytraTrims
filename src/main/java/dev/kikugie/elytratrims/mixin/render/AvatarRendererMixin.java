package dev.kikugie.elytratrims.mixin.render;

import dev.kikugie.elytratrims.cosmetic.ElytraCosmetics;
import dev.kikugie.fletching_table.mixin.MixinEnvironment;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@MixinEnvironment(type = MixinEnvironment.Env.CLIENT)
@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
        at = @At("TAIL")
    )
    private void attachElytraCosmetic(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        state.setData(ElytraCosmetics.RENDER_KEY, ElytraCosmetics.forEntity(entity));
    }
}
