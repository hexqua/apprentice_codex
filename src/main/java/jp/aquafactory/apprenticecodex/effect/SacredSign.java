package jp.aquafactory.apprenticecodex.effect;

import io.redspace.ironsspellbooks.effect.MagicMobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public final class SacredSign extends MagicMobEffect {
    public SacredSign() {
        // Guidedの誘導管理には登録せず、専用の目印としてだけ使用する。
        super(MobEffectCategory.HARMFUL, 16239960);
    }
}
