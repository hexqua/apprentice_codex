package jp.aquafactory.apprenticecodex.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.stln.magitech.content.item.armor.FlamglideStriderItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = FlamglideStriderItem.class, remap = false)
public abstract class MagitechExoskeletonCooldownMixin {
    @WrapOperation(method = "longJump", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemCooldowns;addCooldown(Lnet/minecraft/world/item/Item;I)V", remap = true))
    private static void apprenticecodex$exoskeletonCooldown(ItemCooldowns cooldowns, Item item, int ticks, Operation<Void> original) {
        // 元の靴の20tickは維持し、追加加速の期間に合わせて外骨格だけ12tickへ変更する。
        original.call(cooldowns, item, item == ItemRegistry.MAGI_ENGINEERS_LEG_EXOSKELETON.get() ? 12 : ticks);
    }
}
