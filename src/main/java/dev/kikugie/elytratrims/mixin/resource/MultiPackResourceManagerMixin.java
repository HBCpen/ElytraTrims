package dev.kikugie.elytratrims.mixin.resource;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.kikugie.elytratrims.resource.pack.ETRuntimePack;
import dev.kikugie.elytratrims.resource.pack.ETRuntimePackUtilsKt;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(MultiPackResourceManager.class)
public class MultiPackResourceManagerMixin {

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/util/List;copyOf(Ljava/util/Collection;)Ljava/util/List;"))
    private void injectElytraTrimsPacks(PackType type, List<PackResources> packs, CallbackInfo ci, @Local(argsOnly = true) LocalRef<List<PackResources>> packsRef) {
        if (ETRuntimePackUtilsKt.isWrapped(packs) || !ETRuntimePackUtilsKt.hasElytraTrimsPack(packs))
            return;

        ETRuntimePack injected = ETRuntimePack.of(type, new MultiPackResourceManager(type, ETRuntimePackUtilsKt.wrap(packs)));
        if (injected == null)
            return;

        List<PackResources> copy = new ArrayList<>(packs);
        copy.add(injected);
        packsRef.set(copy);
    }
}
