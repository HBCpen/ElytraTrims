package dev.kikugie.elytratrims.mixin.resource;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.kikugie.elytratrims.resource.reload.ElytraTrimsAtlas;
import dev.kikugie.fletching_table.annotation.MixinEnvironment;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.client.resources.model.AtlasManager.AtlasConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

@MixinEnvironment(type = MixinEnvironment.Env.CLIENT)
@Mixin(AtlasManager.class)
public class AtlasManagerMixin {
    @ModifyExpressionValue(method = "<clinit>", at = @At(value = "INVOKE", target = "Ljava/util/List;of([Ljava/lang/Object;)Ljava/util/List;"))
    private static List<AtlasConfig> registerElytraTrimsAtlas(List<AtlasConfig> original) {
        ArrayList<AtlasConfig> mutableCopy = new ArrayList<>(original);
        mutableCopy.add(ElytraTrimsAtlas.createAtlasConfig());
        return mutableCopy;
    }
}
