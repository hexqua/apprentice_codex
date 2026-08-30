package jp.aquafactory.apprenticecodex.spell.bloodbrand;

import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashSet;

public final class BloodBrandBurst {
    private static final float HIGANBANA_MULTIPLIER = 1.5F;
    private static final DustParticleOptions BLOOD_DUST =
            new DustParticleOptions(new Vector3f(0.7F, 0.02F, 0.04F), 1.35F);
    private static final int DUST_COUNT = 72;
    private static final int EFFECT_COUNT = 56;

    private BloodBrandBurst() {
    }

    public static void burst(ServerLevel level, LivingEntity origin, LivingEntity caster,
                             BloodBrandState state, boolean higanbanaEnhanced) {
        var multiplier = higanbanaEnhanced ? HIGANBANA_MULTIPLIER : 1.0F;
        var range = state.range() * multiplier;
        var damage = state.burstDamage() * multiplier;
        var center = origin.getBoundingBox().getCenter();
        var area = new AABB(center, center).inflate(range);
        var damageType = higanbanaEnhanced
                ? DamageTypes.BLOOD_BRAND_HIGANBANA_BURST
                : DamageTypes.BLOOD_BRAND_BURST;
        var damageSource = CombatTools.getDamageSource(level, origin, caster, damageType);
        var damagedTargets = new HashSet<java.util.UUID>();
        var healRate = higanbanaEnhanced ? 1.0F : 0.5F;
        var totalHealing = 0.0F;

        for (var rawTarget : level.getEntities((Entity) null, area, entity -> isCandidate(entity, origin, caster))) {
            var resolved = CombatTools.resolutePartEntity(rawTarget);
            if (!(resolved instanceof LivingEntity target) || target == origin
                    || !damagedTargets.add(target.getUUID())) {
                continue;
            }
            if (distanceToBoundingBox(center, target.getBoundingBox()) > range) {
                continue;
            }
            if (!Utils.hasLineOfSight(level, center, target.getBoundingBox().getCenter(), false)) {
                continue;
            }

            var healthBefore = target.getHealth();
            var damaged = CombatTools.applyDamage(
                    target,
                    damage,
                    damageSource,
                    SpellRegistry.BLOOD_BRAND.get().getSchoolType(),
                    CombatTools.KnockbackTypes.NO_KNOCKBACK
            );
            if (damaged) {
                // 余剰ダメージでは回復せず、対象が実際に失った通常体力だけを吸収する。
                totalHealing += Math.max(0.0F, healthBefore - target.getHealth()) * healRate;
            }
        }
        if (totalHealing > 0.0F) {
            caster.heal(totalHealing);
        }

        spawnParticles(level, center, range);
        AudioTools.playSoundFromPosition(
                level,
                center,
                io.redspace.ironsspellbooks.registries.SoundRegistry.BLOOD_EXPLOSION.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F,
                0.04F
        );
    }

    private static boolean isCandidate(Entity entity, LivingEntity origin, LivingEntity caster) {
        if (!entity.isAlive() || entity == origin || entity instanceof Player) {
            return false;
        }
        return CombatTools.isValidCombatTarget(CombatTools.resolutePartEntity(entity), caster);
    }

    private static double distanceToBoundingBox(Vec3 point, AABB box) {
        var x = distanceOutsideAxis(point.x, box.minX, box.maxX);
        var y = distanceOutsideAxis(point.y, box.minY, box.maxY);
        var z = distanceOutsideAxis(point.z, box.minZ, box.maxZ);
        return Math.sqrt(x * x + y * y + z * z);
    }

    private static double distanceOutsideAxis(double value, double min, double max) {
        if (value < min) {
            return min - value;
        }
        return value > max ? value - max : 0.0D;
    }

    private static void spawnParticles(ServerLevel level, Vec3 center, double range) {
        spawnBloodDustCloud(level, center, range);
        spawnEntityEffectCloud(level, center, range);
    }

    private static void spawnBloodDustCloud(ServerLevel level, Vec3 center, double range) {
        for (var i = 0; i < DUST_COUNT; ++i) {
            var direction = new Vec3(
                    level.random.nextGaussian(),
                    level.random.nextGaussian(),
                    level.random.nextGaussian()
            );
            if (direction.lengthSqr() <= 1.0E-8D) {
                direction = new Vec3(0.0D, 1.0D, 0.0D);
            } else {
                direction = direction.normalize();
            }
            var distance = range * Math.cbrt(level.random.nextDouble());
            var position = center.add(direction.scale(distance));
            level.sendParticles(
                    BLOOD_DUST,
                    position.x,
                    position.y,
                    position.z,
                    1,
                    0.025D,
                    0.025D,
                    0.025D,
                    0.01D
            );
        }
    }

    private static void spawnEntityEffectCloud(ServerLevel level, Vec3 center, double range) {
        for (var i = 0; i < EFFECT_COUNT; ++i) {
            var position = randomPositionInSphere(level, center, range);
            // 1.20.1のENTITY_EFFECTは速度引数をRGBとして解釈するため、count=0で色を直接指定する。
            level.sendParticles(
                    ParticleTypes.ENTITY_EFFECT,
                    position.x,
                    position.y,
                    position.z,
                    0,
                    0.24D,
                    0.0D,
                    0.025D,
                    1.0D
            );
        }
    }

    private static Vec3 randomPositionInSphere(ServerLevel level, Vec3 center, double range) {
        var direction = new Vec3(
                level.random.nextGaussian(),
                level.random.nextGaussian(),
                level.random.nextGaussian()
        );
        direction = direction.lengthSqr() <= 1.0E-8D
                ? new Vec3(0.0D, 1.0D, 0.0D)
                : direction.normalize();
        return center.add(direction.scale(range * Math.cbrt(level.random.nextDouble())));
    }
}
