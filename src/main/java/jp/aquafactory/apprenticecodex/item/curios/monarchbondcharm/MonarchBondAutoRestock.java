package jp.aquafactory.apprenticecodex.item.curios.monarchbondcharm;

import jp.aquafactory.apprenticecodex.particle.TransferParticleEffect;
import jp.aquafactory.apprenticecodex.spell.autoturret.AutoTurretEntity;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;

public final class MonarchBondAutoRestock {
    private static final int RESTOCK_INTERVAL = 20;
    private static final double RANGE_SQR = MonarchBondCharm.RANGE * MonarchBondCharm.RANGE;

    private MonarchBondAutoRestock() {
    }

    public static void tick(ServerPlayer wearer) {
        if (wearer.tickCount % RESTOCK_INTERVAL != 0) {
            return;
        }

        var wearerUuid = wearer.getUUID();
        var turrets = wearer.serverLevel().getEntitiesOfClass(
                        AutoTurretEntity.class,
                        wearer.getBoundingBox().inflate(MonarchBondCharm.RANGE),
                        turret -> turret.isAlive()
                                && turret.getRestBulletCount() == 0
                                && wearer.distanceToSqr(turret) <= RANGE_SQR
                                && wearerUuid.equals(turret.getCombatOwnerUuid())
                ).stream()
                .sorted(Comparator
                        .comparingDouble((AutoTurretEntity turret) -> wearer.distanceToSqr(turret))
                        .thenComparing(AutoTurretEntity::getUUID))
                .toList();

        for (var turret : turrets) {
            if (turret.tryRestock(wearer) == AutoTurretEntity.RestockResult.SUCCESS) {
                var start = wearer.getEyePosition().subtract(0.0D, 0.25D, 0.0D);
                var end = turret.position().add(0.0D, AutoTurretEntity.HEIGHT * 0.65D, 0.0D);
                TransferParticleEffect.spawn(
                        wearer.serverLevel(), start, end, TransferParticleEffect.Palette.MANA);
            }
        }
    }
}
