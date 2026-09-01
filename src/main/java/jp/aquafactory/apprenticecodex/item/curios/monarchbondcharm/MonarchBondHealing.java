package jp.aquafactory.apprenticecodex.item.curios.monarchbondcharm;

import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Comparator;
import java.util.List;

public final class MonarchBondHealing {
    public static final double RANGE = 32.0D;
    private static final double RANGE_SQR = RANGE * RANGE;
    private static final double PARTICLE_SPACING = 2.0D;
    private static final int MIN_PARTICLE_COUNT = 4;
    private static final int MAX_PARTICLE_COUNT = 16;
    private static final AdditiveGlowParticleOptions HEALING_SPARK = new AdditiveGlowParticleOptions(
            ParticleRegistry.ADDITIVE_SPARK.get(), 0.11F,
            0.35F, 1.0F, 0.45F, 2, 11, 3, 0.65F, 1.3F,
            0.62F, 0.95F, 0.08F, 0.42F, 0.55F, true
    );
    private static final AdditiveGlowParticleOptions HEALING_RHOMBUS = new AdditiveGlowParticleOptions(
            ParticleRegistry.ADDITIVE_RHOMBUS.get(), 0.16F,
            0.35F, 1.0F, 0.45F, 3, 14, 4, 0.75F, 1.25F,
            0.5F, 0.82F, 0.08F, 0.58F, 0.35F, true
    );

    private MonarchBondHealing() {
    }

    public static void distributeOverflow(ServerPlayer wearer, float requestedHealing) {
        if (requestedHealing <= 0.0F) {
            return;
        }

        var missingWearerHealth = Math.max(0.0F, wearer.getMaxHealth() - wearer.getHealth());
        var remainingHealing = requestedHealing - missingWearerHealth;
        if (remainingHealing <= 0.0F) {
            return;
        }

        for (var target : findTargets(wearer)) {
            var missingTargetHealth = Math.max(0.0F, target.getMaxHealth() - target.getHealth());
            var reservedHealing = Math.min(remainingHealing, missingTargetHealth);
            if (reservedHealing > 0.0F) {
                var previousHealth = target.getHealth();
                target.heal(reservedHealing);
                if (target.getHealth() > previousHealth) {
                    spawnHealingTransferParticles(wearer.serverLevel(), wearer, target);
                }
                remainingHealing -= reservedHealing;
            }
            if (remainingHealing <= 0.0F) {
                break;
            }
        }
    }

    public static void healAll(ServerPlayer wearer) {
        for (var target : findTargets(wearer)) {
            var missingHealth = Math.max(0.0F, target.getMaxHealth() - target.getHealth());
            if (missingHealth > 0.0F) {
                var previousHealth = target.getHealth();
                target.heal(missingHealth);
                if (target.getHealth() > previousHealth) {
                    spawnHealingTransferParticles(wearer.serverLevel(), wearer, target);
                }
            }
        }
    }

    private static void spawnHealingTransferParticles(
            ServerLevel level,
            ServerPlayer wearer,
            LivingEntity target
    ) {
        var start = wearer.getEyePosition().subtract(0.0D, 0.25D, 0.0D);
        var end = target.position().add(0.0D, target.getBbHeight() * 0.65D, 0.0D);
        var travel = end.subtract(start);
        var particleCount = Math.min(
                MAX_PARTICLE_COUNT,
                Math.max(MIN_PARTICLE_COUNT, (int) Math.ceil(travel.length() / PARTICLE_SPACING))
        );
        for (var i = 1; i <= particleCount; ++i) {
            var point = start.add(travel.scale((double) i / (particleCount + 1)));
            var particle = i % 3 == 0 ? HEALING_RHOMBUS : HEALING_SPARK;
            level.sendParticles(particle, point.x, point.y, point.z,
                    1, 0.025D, 0.025D, 0.025D, 0.0D);
        }
    }

    private static List<LivingEntity> findTargets(ServerPlayer wearer) {
        var wearerUuid = wearer.getUUID();
        return wearer.serverLevel().getEntitiesOfClass(
                        LivingEntity.class,
                        wearer.getBoundingBox().inflate(RANGE),
                        target -> target != wearer
                                && target.isAlive()
                                && wearer.distanceToSqr(target) <= RANGE_SQR
                                && (SummonManager.getOwner(target) == wearer
                                || target instanceof MonarchBondHealingTarget healingTarget
                                && wearerUuid.equals(healingTarget.getCombatOwnerUuid()))
                ).stream()
                .sorted(Comparator
                        .comparingDouble((LivingEntity target) -> wearer.distanceToSqr(target))
                        .thenComparing(LivingEntity::getUUID))
                .toList();
    }
}
