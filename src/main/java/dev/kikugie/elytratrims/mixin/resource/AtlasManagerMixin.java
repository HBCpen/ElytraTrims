package dev.kikugie.elytratrims.mixin.resource;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.kikugie.elytratrims.resource.reload.ElytraTrimsAtlas;
import dev.kikugie.fletching_table.annotation.MixinEnvironment;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.client.resources.model.sprite.SpriteId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@MixinEnvironment(type = MixinEnvironment.Env.CLIENT)
@Mixin(AtlasManager.class)
public class AtlasManagerMixin {
    @ModifyExpressionValue(method = "<clinit>", at = @At(value = "INVOKE", target = "Ljava/util/List;of([Ljava/lang/Object;)Ljava/util/List;"))
    private static List<AtlasManager.AtlasConfig> registerElytraTrimsAtlas(List<AtlasManager.AtlasConfig> original) {
        ArrayList<AtlasManager.AtlasConfig> mutableCopy = new ArrayList<>(original);
        mutableCopy.add(ElytraTrimsAtlas.createAtlasConfig());
        return mutableCopy;
    }

    //? if neoforge {
    /*@ModifyExpressionValue(method = "updateSpriteMaps", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/sprite/AtlasManager$PendingStitchResults;joinAndUpload()Ljava/util/Map;"))
    private Map<SpriteId, TextureAtlasSprite> removeElytraTrimsSpritesBluntlyCopiedByNeoForgeBecauseFuckMe(Map<SpriteId, TextureAtlasSprite> original) {
        ElytraTrimsAtlas.removeDuplicateSprites(original);
        return original;
    }
    *///?}
}