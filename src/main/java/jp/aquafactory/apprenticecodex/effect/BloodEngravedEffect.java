package jp.aquafactory.apprenticecodex.effect;

import jp.aquafactory.apprenticecodex.spell.bloodbrand.BloodBrandState;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public class BloodEngravedEffect extends MobEffect {
    public static final int DURATION_TICKS = 20 * 10;

    public BloodEngravedEffect() {
        super(MobEffectCategory.HARMFUL, 0x5A0710);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyEffectTick(@NotNull LivingEntity livingEntity, int amplifier) {
        if (!livingEntity.level().isClientSide && BloodBrandState.get(livingEntity) == null) {
            // 効果だけをコマンド等で付与しても起爆情報を復元できないため、不完全な状態は残さない。
            livingEntity.removeEffect(this);
        }
    }
}
