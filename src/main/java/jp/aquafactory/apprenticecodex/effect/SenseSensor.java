package jp.aquafactory.apprenticecodex.effect;

import io.redspace.ironsspellbooks.effect.MagicMobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class SenseSensor extends MagicMobEffect {
    public SenseSensor() {
        super(MobEffectCategory.BENEFICIAL, 0x00D1D1);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}
