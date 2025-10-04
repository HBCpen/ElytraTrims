package dev.kikugie.elytratrims.mixin.resource;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.kikugie.elytratrims.resource.pack.ETRuntimePack;
import dev.kikugie.elytratrims.resource.pack.ETRuntimePackUtilsKt;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

@Mixin(MultiPackResourceManager.class)
public class MultiPackResourceManagerMixin {
    @ModifyExpressionValue(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/util/List;copyOf(Ljava/util/Collection;)Ljava/util/List;"))
    private List<PackResources> injectElytraTrimsPacks(List<PackResources> packs, @Local(argsOnly = true) PackType type) {
        if (!ETRuntimePackUtilsKt.hasElytraTrimsPack(packs) || ETRuntimePackUtilsKt.hasInjectedPack(packs))
            return packs;

        ETRuntimePack injected = ETRuntimePack.of(type, (MultiPackResourceManager) (Object) this);
        if (injected == null)
            return packs;

        List<PackResources> copy = new ArrayList<>(packs);
        copy.add(injected);
        return copy;
    }
}
