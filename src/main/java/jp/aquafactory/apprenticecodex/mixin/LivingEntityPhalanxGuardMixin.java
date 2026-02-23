package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityPhalanxGuardMixin {
    private static final String VIRTUAL_SHIELD_TAG = "ApprenticeCodexVirtualPhalanxShield";

    @Shadow
    protected ItemStack useItem;

    @Shadow
    public abstract boolean isUsingItem();

    @Shadow
    protected abstract void updateUsingItem(ItemStack pUsingItem);

    @Inject(method = "updatingUsingItem", at = @At("HEAD"), cancellable = true)
    private void apprenticecodex$keepVirtualShieldUse(CallbackInfo ci) {
        if (!((Object) this instanceof Player player)) {
            return;
        }

        if (!player.hasEffect(EffectRegistry.PHALANX_STANCE.get())) {
            return;
        }

        if (!isUsingItem()) {
            return;
        }

        if (!isVirtualShield(useItem)) {
            return;
        }

        // Keep virtual shield use alive while phalanx stance is active.
        updateUsingItem(useItem);
        ci.cancel();
    }

    private static boolean isVirtualShield(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != Items.SHIELD) {
            return false;
        }

        var tag = stack.getTag();
        return tag != null && tag.getBoolean(VIRTUAL_SHIELD_TAG);
    }
}
