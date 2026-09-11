package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntitySacredSignMixin {
    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void apprenticecodex$sacredSignGlow(CallbackInfoReturnable<Boolean> cir) {
        var self = (LivingEntity) (Object) this;
        // Iron'sと同様にserverの発光判定を標準フラグへ同期し、他の発光理由を解除しない。
        if (!self.level().isClientSide && self.hasEffect(EffectRegistry.SACRED_SIGN)) cir.setReturnValue(true);
    }
}
