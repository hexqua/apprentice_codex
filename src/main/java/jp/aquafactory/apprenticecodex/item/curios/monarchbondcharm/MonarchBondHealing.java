package jp.aquafactory.apprenticecodex.item.curios.monarchbondcharm;

import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import jp.aquafactory.apprenticecodex.particle.TransferParticleEffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Comparator;
import java.util.List;

public final class MonarchBondHealing {
    private static final double RANGE_SQR = MonarchBondCharm.RANGE * MonarchBondCharm.RANGE;

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
        TransferParticleEffect.spawn(level, start, end, TransferParticleEffect.Palette.HEALING);
    }

    private static List<LivingEntity> findTargets(ServerPlayer wearer) {
        var wearerUuid = wearer.getUUID();
        return wearer.serverLevel().getEntitiesOfClass(
                        LivingEntity.class,
                        wearer.getBoundingBox().inflate(MonarchBondCharm.RANGE),
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
