package jp.aquafactory.apprenticecodex.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class SenseSensor extends MobEffect {
    public SenseSensor() {
        super(MobEffectCategory.BENEFICIAL, 0x00D1D1);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false;
    }
}
