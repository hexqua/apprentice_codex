package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightScrollcasterGauntletOffhandBridge;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

@Mixin(value = LivingEntityPatch.class, remap = false)
public abstract class EpicFightLivingEntityPatchMixin {
    @Inject(method = "getAdvancedHoldingItemCapability", at = @At("HEAD"), cancellable = true)
    private void apprenticecodex$mirrorGauntletCapability(
            InteractionHand hand,
            CallbackInfoReturnable<CapabilityItem> callback
    ) {
        var entityPatch = (LivingEntityPatch<?>) (Object) this;
        if (EpicFightScrollcasterGauntletOffhandBridge.shouldMirrorMainhand(entityPatch, hand)) {
            callback.setReturnValue(EpicFightScrollcasterGauntletOffhandBridge.getMirroredCapability(entityPatch));
        }
    }

    @Inject(method = "getAttackSpeed", at = @At("HEAD"), cancellable = true)
    private void apprenticecodex$mirrorGauntletAttackSpeed(
            InteractionHand hand,
            CallbackInfoReturnable<Float> callback
    ) {
        var entityPatch = (LivingEntityPatch<?>) (Object) this;
        if (EpicFightScrollcasterGauntletOffhandBridge.shouldMirrorMainhand(entityPatch, hand)) {
            callback.setReturnValue(EpicFightScrollcasterGauntletOffhandBridge.getMirroredAttackSpeed(entityPatch));
        }
    }

    @Inject(method = "getAdvancedHoldingItemStack", at = @At("HEAD"), cancellable = true)
    private void apprenticecodex$mirrorGauntletStack(
            InteractionHand hand,
            CallbackInfoReturnable<ItemStack> callback
    ) {
        var entityPatch = (LivingEntityPatch<?>) (Object) this;
        if (EpicFightScrollcasterGauntletOffhandBridge.shouldMirrorMainhand(entityPatch, hand)) {
            callback.setReturnValue(EpicFightScrollcasterGauntletOffhandBridge.getMirroredStack(entityPatch));
        }
    }

    @Inject(method = "getValidItemInHand", at = @At("HEAD"), cancellable = true)
    private void apprenticecodex$mirrorValidGauntletStack(
            InteractionHand hand,
            CallbackInfoReturnable<ItemStack> callback
    ) {
        var entityPatch = (LivingEntityPatch<?>) (Object) this;
        if (EpicFightScrollcasterGauntletOffhandBridge.shouldMirrorMainhand(entityPatch, hand)) {
            callback.setReturnValue(EpicFightScrollcasterGauntletOffhandBridge.getMirroredStack(entityPatch));
        }
    }
}
