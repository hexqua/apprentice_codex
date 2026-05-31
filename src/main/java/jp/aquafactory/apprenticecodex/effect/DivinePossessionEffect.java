package jp.aquafactory.apprenticecodex.effect;

import io.redspace.ironsspellbooks.effect.MagicMobEffect;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class DivinePossessionEffect extends MagicMobEffect {
    private static final int PARTICLE_INTERVAL_TICKS = 4;
    private static final ElementColor[] ELEMENT_COLORS = {
            new ElementColor(1.0F, 0.16F, 0.08F),
            new ElementColor(0.25F, 0.72F, 1.0F),
            new ElementColor(1.0F, 0.88F, 0.16F),
            new ElementColor(0.18F, 0.95F, 0.34F)
    };

    public DivinePossessionEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFE36A);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % PARTICLE_INTERVAL_TICKS == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return true;
        }

        var random = serverLevel.random;
        for (var index = 0; index < ELEMENT_COLORS.length; ++index) {
            spawnElementParticles(serverLevel, entity, random, ELEMENT_COLORS[index], index);
        }
        return true;
    }

    private static void spawnElementParticles(
            ServerLevel level,
            LivingEntity entity,
            RandomSource random,
            ElementColor color,
            int elementIndex
    ) {
        var angle = (entity.tickCount * 0.18D) + elementIndex * Math.PI * 0.5D;
        var radius = entity.getBbWidth() * 0.55D + 0.28D;
        var baseX = entity.getX() + Math.cos(angle) * radius;
        var baseY = entity.getY() + entity.getBbHeight() * (0.28D + random.nextDouble() * 0.45D);
        var baseZ = entity.getZ() + Math.sin(angle) * radius;
        var outwardX = Math.cos(angle) * 0.035D;
        var outwardZ = Math.sin(angle) * 0.035D;

        level.sendParticles(createRhombusParticle(color), baseX, baseY, baseZ, 1,
                0.04D, 0.05D, 0.04D, 0.012D);
        level.sendParticles(createSparkParticle(color), baseX, baseY, baseZ, 2,
                Math.abs(outwardX) + 0.03D, 0.05D, Math.abs(outwardZ) + 0.03D, 0.02D);
    }

    private static AdditiveGlowParticleOptions createRhombusParticle(ElementColor color) {
        return new AdditiveGlowParticleOptions(
                ParticleRegistry.ADDITIVE_RHOMBUS.get(),
                0.18F,
                color.red(),
                color.green(),
                color.blue(),
                3,
                14,
                5,
                0.75F,
                1.25F,
                0.50F,
                0.82F,
                0.08F,
                0.58F,
                0.35F,
                true
        );
    }

    private static AdditiveGlowParticleOptions createSparkParticle(ElementColor color) {
        return new AdditiveGlowParticleOptions(
                ParticleRegistry.ADDITIVE_SPARK.get(),
                0.11F,
                color.red(),
                color.green(),
                color.blue(),
                2,
                11,
                4,
                0.65F,
                1.30F,
                0.62F,
                0.95F,
                0.08F,
                0.42F,
                0.55F,
                true
        );
    }

    private record ElementColor(float red, float green, float blue) {
    }
}
