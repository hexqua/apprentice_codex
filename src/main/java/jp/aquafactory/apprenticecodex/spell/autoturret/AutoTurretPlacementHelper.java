package jp.aquafactory.apprenticecodex.spell.autoturret;

import jp.aquafactory.apprenticecodex.spell.PlacementHelper;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

final class AutoTurretPlacementHelper {
    private AutoTurretPlacementHelper() {
    }

    public static BlockTargetData captureClientTarget(Player player, double range) {
        return PlacementHelper.captureClientTarget(player, range);
    }

    public static Optional<PlacementResult> resolveClientPreview(Level level, LivingEntity entity, double range) {
        return PlacementHelper.resolveClientPreview(level, entity, range, AutoTurretEntity::makePlacementAabb)
                .map(PlacementResult::from);
    }

    public static Optional<PlacementResult> resolveServer(Level level, LivingEntity entity, ResourceLocation spellId, double range) {
        return PlacementHelper.resolveServer(level, entity, spellId, range, AutoTurretEntity::makePlacementAabb)
                .map(PlacementResult::from);
    }

    public static Optional<PlacementResult> resolve(Level level, BlockTargetData targetData) {
        return PlacementHelper.resolve(level, targetData, AutoTurretEntity::makePlacementAabb)
                .map(PlacementResult::from);
    }

    static boolean hasSupportBelow(Level level, BlockPos placementPos) {
        return PlacementHelper.hasSupportBelow(level, placementPos);
    }

    static double getSupportTopY(Level level, BlockPos placementPos) {
        return PlacementHelper.getSupportTopY(level, placementPos);
    }

    public record PlacementResult(BlockPos blockPos, Vec3 center, AABB placementBox) {
        private static PlacementResult from(PlacementHelper.PlacementResult result) {
            return new PlacementResult(result.blockPos(), result.center(), result.placementBox());
        }
    }
}
