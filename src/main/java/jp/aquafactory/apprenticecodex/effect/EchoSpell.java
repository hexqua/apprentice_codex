package jp.aquafactory.apprenticecodex.effect;

import io.redspace.ironsspellbooks.effect.MagicMobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class EchoSpell extends MagicMobEffect {
    public static final int DURATION_TICKS = 20 * 15;

    public EchoSpell() {
        super(MobEffectCategory.BENEFICIAL, 0x7AD7FF);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false;
    }
}
