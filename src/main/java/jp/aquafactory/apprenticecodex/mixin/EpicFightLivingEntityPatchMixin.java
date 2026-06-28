package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightScrollcasterGauntletOffhandBridge;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSpellchargedGreatswordCompat;
import jp.aquafactory.apprenticecodex.item.spellchargedgreatsword.SpellchargedGreatsword;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.collider.Collider;
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

    @Inject(method = "getColliderMatching", at = @At("RETURN"), cancellable = true)
    private void apprenticecodex$useOverchargedGreatswordCollider(
            InteractionHand hand,
            CallbackInfoReturnable<Collider> callback
    ) {
        if (hand != InteractionHand.MAIN_HAND) {
            return;
        }

        var entityPatch = (LivingEntityPatch<?>) (Object) this;
        if (!(entityPatch.getOriginal() instanceof Player player)) {
            return;
        }

        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof SpellchargedGreatsword) || !SpellchargedGreatsword.isOverchargeActive(stack)) {
            return;
        }

        callback.setReturnValue(EpicFightSpellchargedGreatswordCompat.getOverchargedWeaponCollider(stack.getItem()));
    }
}
