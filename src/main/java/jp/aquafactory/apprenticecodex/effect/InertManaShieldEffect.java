package jp.aquafactory.apprenticecodex.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.common.EffectCure;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public final class InertManaShieldEffect extends MobEffect {
    public InertManaShieldEffect() {
        super(MobEffectCategory.HARMFUL, 0x2B0B4F);
    }

    @Override
    public void fillEffectCures(@NotNull Set<EffectCure> cures, @NotNull MobEffectInstance effectInstance) {
        // Mana Shieldへの対抗状態を牛乳で即座に解除できないよう、標準のcureを意図的に登録しない.
    }
}
