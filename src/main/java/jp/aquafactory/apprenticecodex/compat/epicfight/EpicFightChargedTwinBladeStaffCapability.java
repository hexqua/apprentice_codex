package jp.aquafactory.apprenticecodex.compat.epicfight;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.UseAnim;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.Style;
import yesman.epicfight.world.capabilities.item.WeaponCapability;

import java.util.HashMap;
import java.util.Map;

public final class EpicFightChargedTwinBladeStaffCapability extends WeaponCapability {
    private static final Map<LivingMotion, AnimationManager.AnimationAccessor<? extends StaticAnimation>> THROW_MOTION_MODIFIERS =
            Map.of(
                    LivingMotions.AIM, Animations.BIPED_JAVELIN_AIM,
                    LivingMotions.SHOT, Animations.BIPED_JAVELIN_THROW
            );

    public EpicFightChargedTwinBladeStaffCapability(CapabilityItem.Builder builder) {
        super(builder);
    }

    @Override
    public Style getStyle(LivingEntityPatch<?> entityPatch) {
        return CapabilityItem.Styles.TWO_HAND;
    }

    @Override
    public LivingMotion getLivingMotion(LivingEntityPatch<?> entityPatch, InteractionHand hand) {
        var livingEntity = (LivingEntity) entityPatch.getOriginal();
        if (hand == InteractionHand.MAIN_HAND
                && livingEntity.isUsingItem()
                && livingEntity.getUseItem().getUseAnimation() == UseAnim.SPEAR) {
            return LivingMotions.AIM;
        }

        return null;
    }

    @Override
    public UseAnim getUseAnimation(LivingEntityPatch<?> entityPatch) {
        return UseAnim.NONE;
    }

    @Override
    public Map<LivingMotion, AnimationManager.AnimationAccessor<? extends StaticAnimation>> getLivingMotionModifier(
            LivingEntityPatch<?> entityPatch,
            InteractionHand hand
    ) {
        var modifiers = new HashMap<>(super.getLivingMotionModifier(entityPatch, hand));
        modifiers.remove(LivingMotions.BLOCK);

        if (hand == InteractionHand.MAIN_HAND) {
            modifiers.putAll(THROW_MOTION_MODIFIERS);
        }

        return modifiers;
    }
}
