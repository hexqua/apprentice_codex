package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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
        // Mixin適用後は this が LivingEntity 実体を指す。明示キャストで静的解析の誤判定を避ける。
        var self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player)) {
            return;
        }

        if (!player.hasEffect(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(EffectRegistry.PHALANX_STANCE.get()))) {
            return;
        }

        if (!isUsingItem()) {
            return;
        }

        if (!apprentice_codex$isVirtualShield(useItem)) {
            return;
        }

        // Keep virtual shield use alive while phalanx stance is active.
        updateUsingItem(useItem);
        ci.cancel();
    }

    @Unique
    private static boolean apprentice_codex$isVirtualShield(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != Items.SHIELD) {
            return false;
        }

        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }
        return customData.copyTag().getBoolean(VIRTUAL_SHIELD_TAG);
    }
}
