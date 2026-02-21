package jp.aquafactory.apprenticecodex.spell.featherrush;

import jp.aquafactory.apprenticecodex.spell.assistwings.AssistWingsWingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class FeatherRushWingEntity extends AssistWingsWingEntity {
    public FeatherRushWingEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public FeatherRushWingEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel, owner);
    }

    @Override
    public void tickOnServer(ServerLevel level) {
        if (!(getOwner() instanceof Player owner) || !owner.isAlive()) {
            discard();
            return;
        }

        followTargetPosition(getBackPosition(owner));

        if (!owner.isInWaterOrBubble() && !owner.isFallFlying() && !owner.getAbilities().flying &&
                !owner.onClimbable() && !owner.isPassenger() && !owner.isSwimming() && !owner.onGround()) {
            owner.fallDistance = 0;
            if (!owner.isShiftKeyDown()) {
                var effect = owner.getEffect(MobEffects.SLOW_FALLING);
                if (effect == null || effect.getDuration() < 10) {
                    owner.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20, 0, true, false, false));
                }
            }
        }

        setYRot(owner.getYRot());
        setXRot(0);
        setRot(getYRot(), getXRot());
    }
}
