package jp.aquafactory.apprenticecodex.effect;

import io.redspace.ironsspellbooks.effect.MagicMobEffect;
import jp.aquafactory.apprenticecodex.spell.spectralwing.SpectralWingFlightEvent;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class SpectralWingEffect extends MagicMobEffect {
    public SpectralWingEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xD8F4FF);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false;
    }

    @Override
    public void onEffectRemoved(LivingEntity entity, int amplifier) {
        SpectralWingFlightEvent.onSpectralWingEffectRemoved(entity);
    }
}
