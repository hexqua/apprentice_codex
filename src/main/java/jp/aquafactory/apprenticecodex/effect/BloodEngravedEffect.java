package jp.aquafactory.apprenticecodex.effect;

import jp.aquafactory.apprenticecodex.registry.AttachmentRegistry;
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
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity livingEntity, int amplifier) {
        if (livingEntity.level().isClientSide) {
            return true;
        }

        // 効果だけをコマンド等で付与しても起爆情報を復元できないため、不完全な状態は残さない。
        return livingEntity.getExistingDataOrNull(AttachmentRegistry.BLOOD_BRAND_STATE) != null;
    }
}
