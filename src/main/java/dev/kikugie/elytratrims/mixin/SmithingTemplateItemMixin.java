package dev.kikugie.elytratrims.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.kikugie.elytratrims.CommonsKt;
import kotlin.collections.CollectionsKt;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.SmithingTemplateItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(SmithingTemplateItem.class)
public class SmithingTemplateItemMixin {
    @Unique
    private static final Identifier EMPTY_SLOT_ELYTRA = CommonsKt.elytratrims("container/slot/elytra");

    @ModifyExpressionValue(method = "createTrimmableArmorIconList", at = @At(value = "INVOKE", target = "Ljava/util/List;of(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/List;"))
    private static List<Identifier> addElytraTexture(List<Identifier> original) {
        return original.contains(EMPTY_SLOT_ELYTRA) ? original : CollectionsKt.plus(original, EMPTY_SLOT_ELYTRA);
    }
}
