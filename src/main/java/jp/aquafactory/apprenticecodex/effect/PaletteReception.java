package jp.aquafactory.apprenticecodex.effect;

import io.redspace.ironsspellbooks.effect.MagicMobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class PaletteReception extends MagicMobEffect {
    public PaletteReception() {
        super(MobEffectCategory.BENEFICIAL, 0xEEDBFF);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}
