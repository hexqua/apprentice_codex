package jp.aquafactory.apprenticecodex.spell.inscribeice;

import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public final class InscribeIceBurst {
    public static final float BLAST_HALF_EXTENT = 2.5F;
    private static final float CHAIN_DAMAGE_MULTIPLIER = 0.5F;

    private InscribeIceBurst() {
    }

    public static void burstFromDagger(ServerLevel level, LivingEntity origin, @Nullable Entity projectile,
                                       @Nullable Entity owner, float burstDamage) {
        burst(level, origin, projectile, owner, burstDamage, burstDamage * CHAIN_DAMAGE_MULTIPLIER, new HashSet<>());
    }

    public static void burstChain(ServerLevel level, LivingEntity origin, @Nullable Entity sourceEntity,
                                  @Nullable Entity owner, float chainBurstDamage, Set<Integer> processedEntityIds) {
        burst(level, origin, sourceEntity, owner, chainBurstDamage, chainBurstDamage, processedEntityIds);
    }

    public static AdditiveGlowParticleOptions createTrailSparkParticle() {
        return new AdditiveGlowParticleOptions(
                ParticleRegistry.ADDITIVE_SPARK.get(),
                0.13F,
                0.3F,
                0.8F,
                1.0F,
                2,
                12,
                4,
                0.6F,
                1.2F,
                0.45F,
                0.9F,
                0.1F,
                0.7F,
                0.1F,
                true
        );
    }

    private static void burst(ServerLevel level, LivingEntity origin, @Nullable Entity sourceEntity,
                              @Nullable Entity owner, float damage, float chainDamage,
                              Set<Integer> processedEntityIds) {
        if (origin.isRemoved()) {
            return;
        }
        if (!processedEntityIds.add(origin.getId())) {
            return;
        }

        origin.removeEffect(EffectRegistry.NOTCHED_FROZEN.get());

        var center = origin.getBoundingBox().getCenter();
        var damageSource = createDamageSource(level, sourceEntity != null ? sourceEntity : owner, owner);
        var area = new AABB(
                center.x - BLAST_HALF_EXTENT, center.y - BLAST_HALF_EXTENT, center.z - BLAST_HALF_EXTENT,
                center.x + BLAST_HALF_EXTENT, center.y + BLAST_HALF_EXTENT, center.z + BLAST_HALF_EXTENT
        );
        var damagedIds = new HashSet<Integer>();

        for (var rawTarget : level.getEntities((Entity) null, area, entity -> isBlastCandidate(entity, owner))) {
            var resolved = CombatTools.resolutePartEntity(rawTarget);
            if (!(resolved instanceof LivingEntity livingTarget)) {
                continue;
            }
            if (!damagedIds.add(livingTarget.getId())) {
                continue;
            }

            var damaged = CombatTools.applyDamage(
                    livingTarget,
                    damage,
                    damageSource,
                    SpellRegistry.INSCRIBE_ICE.get().getSchoolType(),
                    CombatTools.KnockbackTypes.NO_KNOCKBACK
            );
            if (damaged && livingTarget.hasEffect(EffectRegistry.NOTCHED_FROZEN.get())) {
                burstChain(level, livingTarget, sourceEntity, owner, chainDamage, processedEntityIds);
            }
        }

        spawnBlastParticles(level, center);
        AudioTools.playSoundFromPosition(level, center, SoundRegistry.FROZEN_RUNE.get(), SoundSource.PLAYERS, 1.0F, 1.0F, 0.04F);
    }

    private static boolean isBlastCandidate(Entity entity, @Nullable Entity owner) {
        if (!entity.isAlive() || entity instanceof Player) {
            return false;
        }
        var resolved = CombatTools.resolutePartEntity(entity);
        return CombatTools.isValidCombatTarget(resolved, owner);
    }

    private static DamageSource createDamageSource(ServerLevel level, @Nullable Entity sourceEntity, @Nullable Entity owner) {
        if (sourceEntity != null && owner != null) {
            return CombatTools.getDamageSource(level, sourceEntity, owner, DamageTypes.INSCRIBE_ICE_BURST);
        }
        if (sourceEntity != null) {
            return CombatTools.getDamageSource(level, sourceEntity, DamageTypes.INSCRIBE_ICE_BURST);
        }

        var registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        var holder = registry.getHolder(DamageTypes.INSCRIBE_ICE_BURST)
                .orElseGet(() -> (Holder.Reference<DamageType>) level.damageSources().genericKill().typeHolder());
        return new DamageSource(holder);
    }

    private static void spawnBlastParticles(ServerLevel level, Vec3 center) {
        var random = level.random;
        var snowDust = new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SNOW.defaultBlockState());
        var spark = new AdditiveGlowParticleOptions(
                ParticleRegistry.ADDITIVE_SPARK.get(),
                0.22F,
                0.25F,
                1.0F,
                1.0F,
                3,
                18,
                8,
                0.65F,
                1.35F,
                0.7F,
                1.0F,
                0.1F,
                0.65F,
                0.15F,
                true
        );

        for (int i = 0; i < 96; i++) {
            var x = center.x + Mth.nextDouble(random, -BLAST_HALF_EXTENT, BLAST_HALF_EXTENT);
            var y = center.y + Mth.nextDouble(random, -BLAST_HALF_EXTENT, BLAST_HALF_EXTENT);
            var z = center.z + Mth.nextDouble(random, -BLAST_HALF_EXTENT, BLAST_HALF_EXTENT);
            level.sendParticles(snowDust, x, y, z, 1, 0.05D, 0.05D, 0.05D, 0.02D);
        }
        level.sendParticles(spark, center.x, center.y, center.z, 64, BLAST_HALF_EXTENT, BLAST_HALF_EXTENT, BLAST_HALF_EXTENT, 0.03D);
    }
}
