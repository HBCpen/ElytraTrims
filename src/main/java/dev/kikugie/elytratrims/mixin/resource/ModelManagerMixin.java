package dev.kikugie.elytratrims.mixin.resource;

//? if <1.21.9 {
/*import dev.kikugie.elytratrims.resource.reload.ElytraTrimsAtlas;
import net.minecraft.client.resources.model.ModelManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.HashMap;
import java.util.Map;

@Mixin(ModelManager.class)
public class ModelManagerMixin {
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/AtlasSet;<init>(Ljava/util/Map;Lnet/minecraft/client/renderer/texture/TextureManager;)V"))
    private static Map<net.minecraft.resources.ResourceLocation, net.minecraft.resources.ResourceLocation> injectETAtlas(
        Map<net.minecraft.resources.ResourceLocation, net.minecraft.resources.ResourceLocation> atlasMap
    ) {
        Map<net.minecraft.resources.ResourceLocation, net.minecraft.resources.ResourceLocation> mutable = new HashMap<>(atlasMap);
        mutable.put(ElytraTrimsAtlas.ATLAS_SHEET, ElytraTrimsAtlas.ATLAS_ID);
        return mutable;
    }
}
*///?}
