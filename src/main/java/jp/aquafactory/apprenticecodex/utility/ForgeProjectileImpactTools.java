package jp.aquafactory.apprenticecodex.utility;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.ForgeEventFactory;

public final class ForgeProjectileImpactTools {
    private ForgeProjectileImpactTools() {
    }

    /**
     * Forge 1.20.1 の実キャンセルと ImpactResult を分離し、非貫通投射物向けの動作へ変換する。
     */
    public static ImpactAction resolveImpactAction(Projectile projectile, HitResult hitResult) {
        var result = ForgeEventFactory.onProjectileImpactResultNullable(projectile, hitResult);
        if (result == null) {
            return ImpactAction.CONTINUE;
        }

        return switch (result) {
            case SKIP_ENTITY -> hitResult.getType() == HitResult.Type.ENTITY
                    ? ImpactAction.CONTINUE
                    : ImpactAction.PROCESS;
            case STOP_AT_CURRENT_NO_DAMAGE -> ImpactAction.STOP_WITHOUT_DAMAGE;
            case DEFAULT, STOP_AT_CURRENT -> ImpactAction.PROCESS;
        };
    }

    public enum ImpactAction {
        CONTINUE,
        PROCESS,
        STOP_WITHOUT_DAMAGE
    }
}
