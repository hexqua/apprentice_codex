package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.event.KnockbackControlEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityBulwarkGreatshieldPushMixin {
    @Inject(method = "push(DDD)V", at = @At("HEAD"), cancellable = true)
    private void apprenticecodex$ignorePushOnBulwarkBlockTick(double x, double y, double z, CallbackInfo ci) {
        //noinspection ConstantValue
        if ((Object) this instanceof LivingEntity livingEntity
                && KnockbackControlEvent.shouldIgnorePushThisTick(livingEntity)) {
            // ラヴェジャーはLivingKnockBackEventを通さず直接pushするため、ブロック成功tickだけpushそのものを止める.
            ci.cancel();
        }
    }
}
