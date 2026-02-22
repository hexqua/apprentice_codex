package jp.aquafactory.apprenticecodex.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class PaletteReception extends MobEffect {
    public PaletteReception() {
        super(MobEffectCategory.BENEFICIAL, 0xEEDBFF);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}
