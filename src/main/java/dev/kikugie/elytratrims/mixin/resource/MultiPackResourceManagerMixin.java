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
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = MultiPackResourceManager.class, priority = 900)
public class MultiPackResourceManagerMixin {
    @ModifyVariable(method = "<init>", at = @At(value = "HEAD"), argsOnly = true)
    private static List<PackResources> injectElytraTrimsPacks(List<PackResources> packs, @Local(argsOnly = true) PackType type) {
        // Only client resources are generated; the (integrated) server is left untouched.
        if (type != PackType.CLIENT_RESOURCES || ETRuntimePackUtilsKt.isWrapped(packs) || !ETRuntimePackUtilsKt.hasElytraTrimsPack(packs))
            return packs;

        ETRuntimePack injected = ETRuntimePack.of(type, new MultiPackResourceManager(type, ETRuntimePackUtilsKt.wrap(packs)));
        if (injected == null)
            return packs;

        List<PackResources> copy = new ArrayList<>(packs);
        copy.add(injected);
        return copy;
    }
}
