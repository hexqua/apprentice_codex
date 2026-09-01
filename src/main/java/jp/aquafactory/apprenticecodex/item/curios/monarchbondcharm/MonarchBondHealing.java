package jp.aquafactory.apprenticecodex.item.curios.monarchbondcharm;

import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Comparator;
import java.util.List;

public final class MonarchBondHealing {
    public static final double RANGE = 32.0D;
    private static final double RANGE_SQR = RANGE * RANGE;

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
                target.heal(reservedHealing);
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
                target.heal(missingHealth);
            }
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
