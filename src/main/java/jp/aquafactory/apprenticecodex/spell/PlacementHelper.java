package jp.aquafactory.apprenticecodex.spell;

import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import jp.aquafactory.apprenticecodex.utility.BlockTargetingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.function.Function;

public final class PlacementHelper {
    private static final int MAX_DOWNWARD_SEARCH = 3;

    private PlacementHelper() {
    }

    public static BlockTargetData captureClientTarget(Player player, double range) {
        var targetData = new BlockTargetData();
        var hit = raycastTargetBlock(player.level(), player, range).orElse(null);
        if (hit == null) {
            return targetData;
        }

        var hitPos = hit.getBlockPos();
        var hitFace = hit.getDirection();
        var hitState = player.level().getBlockState(hitPos);
        var rawPlacePos = hitState.canBeReplaced() ? hitPos : hitPos.relative(hitFace);
        targetData.setTarget(hitPos, hitFace, hit.getLocation(), rawPlacePos, hitFace.getOpposite());
        return targetData;
    }

    public static Optional<PlacementResult> resolveClientPreview(Level level, LivingEntity entity, double range,
                                                                 Function<Vec3, AABB> placementBoxFactory) {
        if (entity instanceof Player player) {
            var clientTarget = captureClientTarget(player, range);
            var resolved = resolve(level, clientTarget, placementBoxFactory);
            if (resolved.isPresent()) {
                return resolved;
            }
        }

        return raycastTargetBlock(level, entity, range)
                .flatMap(hit -> resolve(level, hit.getBlockPos(), hit.getDirection(), placementBoxFactory));
    }

    public static Optional<PlacementResult> resolveServer(Level level, LivingEntity entity, ResourceLocation spellId,
                                                          double range, Function<Vec3, AABB> placementBoxFactory) {
        var clientTarget = BlockTargetingHelper.getValidatedPendingHitTarget(level, entity, spellId, range);
        if (clientTarget.isPresent()) {
            var resolved = resolve(level, clientTarget.get(), placementBoxFactory);
            if (resolved.isPresent()) {
                return resolved;
            }
        }

        return raycastTargetBlock(level, entity, range)
                .flatMap(hit -> resolve(level, hit.getBlockPos(), hit.getDirection(), placementBoxFactory));
    }

    public static Optional<PlacementResult> resolve(Level level, BlockTargetData targetData,
                                                    Function<Vec3, AABB> placementBoxFactory) {
        if (!targetData.hasTarget() || targetData.getHitBlockPos() == null || targetData.getHitFace() == null) {
            return Optional.empty();
        }
        return resolve(level, targetData.getHitBlockPos(), targetData.getHitFace(), placementBoxFactory);
    }

    private static Optional<PlacementResult> resolve(Level level, BlockPos hitPos, Direction hitFace,
                                                     Function<Vec3, AABB> placementBoxFactory) {
        BlockPos candidateBase;
        if (hitFace == Direction.UP) {
            candidateBase = level.getBlockState(hitPos).canBeReplaced() ? hitPos : hitPos.above();
            return buildPlacement(level, candidateBase, placementBoxFactory);
        }

        candidateBase = level.getBlockState(hitPos).canBeReplaced() ? hitPos : hitPos.relative(hitFace);
        for (int offset = 0; offset <= MAX_DOWNWARD_SEARCH; offset++) {
            var candidate = candidateBase.below(offset);
            var result = buildPlacement(level, candidate, placementBoxFactory);
            if (result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();
    }

    private static Optional<PlacementResult> buildPlacement(Level level, BlockPos placementPos,
                                                            Function<Vec3, AABB> placementBoxFactory) {
        if (!level.getBlockState(placementPos).canBeReplaced()) {
            return Optional.empty();
        }

        if (!hasSupportBelow(level, placementPos)) {
            return Optional.empty();
        }

        var center = new Vec3(
                placementPos.getX() + 0.5,
                getSupportTopY(level, placementPos),
                placementPos.getZ() + 0.5
        );
        var placementBox = placementBoxFactory.apply(center);
        if (!level.noCollision(placementBox)) {
            return Optional.empty();
        }

        return Optional.of(new PlacementResult(placementPos.immutable(), center, placementBox));
    }

    public static boolean hasSupportBelow(Level level, BlockPos placementPos) {
        return !level.getBlockState(placementPos.below()).getCollisionShape(level, placementPos.below()).isEmpty();
    }

    public static double getSupportTopY(Level level, BlockPos placementPos) {
        var supportPos = placementPos.below();
        var supportShape = level.getBlockState(supportPos).getCollisionShape(level, supportPos);
        if (supportShape.isEmpty()) {
            return placementPos.getY();
        }
        return supportPos.getY() + supportShape.max(Direction.Axis.Y);
    }

    private static Optional<BlockHitResult> raycastTargetBlock(Level level, LivingEntity entity, double range) {
        var start = entity.getEyePosition(1.0F);
        var end = start.add(entity.getViewVector(1.0F).scale(range));
        var hit = level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                entity
        ));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return Optional.empty();
        }

        return Optional.of(hit);
    }

    public record PlacementResult(BlockPos blockPos, Vec3 center, AABB placementBox) {
    }
}
