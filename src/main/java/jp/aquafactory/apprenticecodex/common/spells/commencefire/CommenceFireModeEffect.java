package jp.aquafactory.apprenticecodex.common.spells.commencefire;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class CommenceFireModeEffect extends MobEffect {
    public CommenceFireModeEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x3A3AFF);
    }

    @Override
    public boolean isDurationEffectTick(int pDuration, int pAmplifier) {
        return false;
    }
}
