package jp.aquafactory.apprenticecodex.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class SpectralWingEffect extends MobEffect {
    public SpectralWingEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xD8F4FF);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false;
    }
}
