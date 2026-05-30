package jp.aquafactory.apprenticecodex.effect;

import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class NotchedFrozenEffect extends MobEffect {
    public static final int DURATION_TICKS = 20 * 15;
    public static final int MAX_STACK_AMPLIFIER = 1;

    public NotchedFrozenEffect() {
        super(MobEffectCategory.HARMFUL, 0x8DEBFF);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity livingEntity, int amplifier) {
        var frozenTicks = livingEntity.getTicksFrozen();
        if (frozenTicks > 0) {
            livingEntity.setTicksFrozen(frozenTicks + 2);
        }

        if (livingEntity.level() instanceof ServerLevel serverLevel && livingEntity.tickCount % 8 == 0) {
            var random = serverLevel.random;
            var box = livingEntity.getBoundingBox();
            var x = box.minX + random.nextDouble() * box.getXsize();
            var y = box.minY + random.nextDouble() * box.getYsize();
            var z = box.minZ + random.nextDouble() * box.getZsize();
            serverLevel.sendParticles(createSparkParticle(), x, y, z, 1, 0.05D, 0.05D, 0.05D, 0.01D);
        }
        return true;
    }

    private static AdditiveGlowParticleOptions createSparkParticle() {
        return new AdditiveGlowParticleOptions(
                ParticleRegistry.ADDITIVE_SPARK.get(),
                0.12F,
                0.35F,
                0.85F,
                1.0F,
                2,
                10,
                4,
                0.65F,
                1.15F,
                0.45F,
                0.9F,
                0.1F,
                0.65F,
                0.1F,
                true
        );
    }
}
