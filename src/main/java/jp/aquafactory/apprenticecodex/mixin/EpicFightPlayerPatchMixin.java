package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightScrollcasterGauntletOffhandBridge;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;

@Mixin(value = PlayerPatch.class, remap = false)
public abstract class EpicFightPlayerPatchMixin {
    @Inject(method = "getDamageSource", at = @At("HEAD"), cancellable = true)
    private void apprenticecodex$mirrorGauntletDamageSource(
            AnimationManager.AnimationAccessor<? extends StaticAnimation> animation,
            InteractionHand hand,
            CallbackInfoReturnable<EpicFightDamageSource> callback
    ) {
        var playerPatch = (PlayerPatch<?>) (Object) this;
        if (EpicFightScrollcasterGauntletOffhandBridge.shouldMirrorMainhand(playerPatch, hand)) {
            callback.setReturnValue(EpicFightScrollcasterGauntletOffhandBridge.getMirroredDamageSource(
                    playerPatch,
                    animation
            ));
        }
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void apprenticecodex$mirrorGauntletAttack(
            EpicFightDamageSource damageSource,
            Entity target,
            InteractionHand hand,
            CallbackInfoReturnable<AttackResult> callback
    ) {
        var playerPatch = (PlayerPatch<?>) (Object) this;
        if (EpicFightScrollcasterGauntletOffhandBridge.shouldMirrorMainhand(playerPatch, hand)) {
            callback.setReturnValue(EpicFightScrollcasterGauntletOffhandBridge.attackWithMirroredMainhand(
                    playerPatch,
                    damageSource,
                    target
            ));
        }
    }
}
