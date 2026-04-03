package jp.aquafactory.apprenticecodex.spell.healingbloom;

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

final class HealingBloomPlacementHelper {
    private static final int MAX_DOWNWARD_SEARCH = 1;

    private HealingBloomPlacementHelper() {
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

    public static Optional<PlacementResult> resolveClientPreview(Level level, LivingEntity entity, double range) {
        if (entity instanceof Player player) {
            var clientTarget = captureClientTarget(player, range);
            var resolved = resolve(level, clientTarget);
            if (resolved.isPresent()) {
                return resolved;
            }
        }

        return raycastTargetBlock(level, entity, range)
                .flatMap(hit -> resolve(level, hit.getBlockPos(), hit.getDirection()));
    }

    public static Optional<PlacementResult> resolveServer(Level level, LivingEntity entity, ResourceLocation spellId, double range) {
        var clientTarget = BlockTargetingHelper.getValidatedPendingHitTarget(level, entity, spellId, range);
        if (clientTarget.isPresent()) {
            var resolved = resolve(level, clientTarget.get());
            if (resolved.isPresent()) {
                return resolved;
            }
        }

        return raycastTargetBlock(level, entity, range)
                .flatMap(hit -> resolve(level, hit.getBlockPos(), hit.getDirection()));
    }

    public static Optional<PlacementResult> resolve(Level level, BlockTargetData targetData) {
        if (!targetData.hasTarget() || targetData.getHitBlockPos() == null || targetData.getHitFace() == null) {
            return Optional.empty();
        }
        return resolve(level, targetData.getHitBlockPos(), targetData.getHitFace());
    }

    public static Optional<PlacementResult> resolve(Level level, BlockPos placementPos) {
        return buildPlacement(level, placementPos);
    }

    private static Optional<PlacementResult> resolve(Level level, BlockPos hitPos, Direction hitFace) {
        BlockPos candidateBase;
        if (hitFace == Direction.UP) {
            candidateBase = level.getBlockState(hitPos).canBeReplaced() ? hitPos : hitPos.above();
            return buildPlacement(level, candidateBase);
        }

        candidateBase = level.getBlockState(hitPos).canBeReplaced() ? hitPos : hitPos.relative(hitFace);
        for (int offset = 0; offset <= MAX_DOWNWARD_SEARCH; ++offset) {
            var candidate = candidateBase.below(offset);
            var result = buildPlacement(level, candidate);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    private static Optional<PlacementResult> buildPlacement(Level level, BlockPos placementPos) {
        if (!level.getBlockState(placementPos).canBeReplaced()) {
            return Optional.empty();
        }

        var lightPos = placementPos.above();
        if (!level.getBlockState(lightPos).canBeReplaced()) {
            return Optional.empty();
        }

        var center = new Vec3(
                placementPos.getX() + 0.5,
                placementPos.getY(),
                placementPos.getZ() + 0.5
        );
        var placementBox = HealingBloomEntity.makePlacementAabb(center);
        if (!level.noCollision(placementBox)) {
            return Optional.empty();
        }

        return Optional.of(new PlacementResult(placementPos.immutable(), center, placementBox));
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
