package jp.aquafactory.apprenticecodex.effect;

import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class GravityBound extends MobEffect {
    private static final double DOWNWARD_ACCELERATION = 0.55D;
    private static final double MIN_FALL_SPEED = -1.25D;
    private static final double MAX_FALL_SPEED = -3.0D;

    public GravityBound() {
        super(MobEffectCategory.HARMFUL, 0x2B0B4F);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyEffectTick(@NotNull LivingEntity livingEntity, int amplifier) {
        if (isDenied(livingEntity)) {
            livingEntity.removeEffect(EffectRegistry.GRAVITY_BOUND.get());
            return;
        }

        var movement = livingEntity.getDeltaMovement();
        var nextY = Math.max(MAX_FALL_SPEED, Math.min(movement.y, 0.0D) - DOWNWARD_ACCELERATION);
        if (!livingEntity.onGround()) {
            nextY = Math.min(nextY, MIN_FALL_SPEED);
        }

        livingEntity.setDeltaMovement(new Vec3(movement.x, nextY, movement.z));
        livingEntity.hasImpulse = true;
        livingEntity.hurtMarked = true;
        livingEntity.setNoGravity(false);
        livingEntity.fallDistance = Math.max(livingEntity.fallDistance, 0.0F);
    }

    private static boolean isDenied(LivingEntity livingEntity) {
        return livingEntity instanceof EnderDragon
                || livingEntity.getType().is(TagRegistry.EntityTypes.GRAVITY_BOUND_DENYLIST);
    }
}
