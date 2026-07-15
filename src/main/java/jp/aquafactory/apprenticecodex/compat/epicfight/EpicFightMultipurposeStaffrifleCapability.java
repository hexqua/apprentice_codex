package jp.aquafactory.apprenticecodex.compat.epicfight;

import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifle;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.UseAnim;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.RangedWeaponCapability;
import yesman.epicfight.world.capabilities.item.Style;

public final class EpicFightMultipurposeStaffrifleCapability extends RangedWeaponCapability {
    public EpicFightMultipurposeStaffrifleCapability(RangedWeaponCapability.Builder builder) {
        super(builder);
    }

    @Override
    public Style getStyle(LivingEntityPatch<?> entityPatch) {
        return CapabilityItem.Styles.TWO_HAND;
    }

    @Override
    public LivingMotion getLivingMotion(LivingEntityPatch<?> entityPatch, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !entityPatch.getEntityState().canUseItem()) {
            return null;
        }

        var livingEntity = (LivingEntity) entityPatch.getOriginal();
        if (!(livingEntity.getMainHandItem().getItem() instanceof MultipurposeStaffrifle)) {
            return null;
        }

        return livingEntity.isUsingItem() ? LivingMotions.AIM : null;
    }

    @Override
    public UseAnim getUseAnimation(LivingEntityPatch<?> entityPatch) {
        return UseAnim.NONE;
    }
}
