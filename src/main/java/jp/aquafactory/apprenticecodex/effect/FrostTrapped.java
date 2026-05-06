package jp.aquafactory.apprenticecodex.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class FrostTrapped extends MobEffect {
    private static final int FROZEN_TICKS = 300;

    public FrostTrapped() {
        super(MobEffectCategory.HARMFUL, 0x55FFFF);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity livingEntity, int amplifier) {
        livingEntity.setTicksFrozen(FROZEN_TICKS);
    }
}
