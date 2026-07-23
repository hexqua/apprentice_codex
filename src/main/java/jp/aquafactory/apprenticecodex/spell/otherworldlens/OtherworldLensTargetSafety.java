package jp.aquafactory.apprenticecodex.spell.otherworldlens;

import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Optional;

final class OtherworldLensTargetSafety {
    private static final double RANGE_EPSILON = 0.25;
    private static final double HIT_LOCATION_EPSILON_SQ = 0.25 * 0.25;

    private OtherworldLensTargetSafety() {
    }

    static Optional<ValidatedTarget> validate(Level level, LivingEntity caster, BlockTargetData target, double range) {
        if (!hasCompleteFiniteTarget(target)) {
            return Optional.empty();
        }

        var hitPos = target.getHitBlockPos();
        var hitFace = target.getHitFace();
        if (hitPos == null || hitFace == null) {
            return Optional.empty();
        }

        var placePos = hitPos.relative(hitFace);
        if (!placePos.equals(target.getPlacePos()) || target.getPlaceFacing() != hitFace.getOpposite()) {
            return Optional.empty();
        }
        if (!level.getBlockState(placePos).isAir()) {
            return Optional.empty();
        }

        var serverHit = caster.pick(range + RANGE_EPSILON, 1.0F, false);
        if (serverHit.getType() != HitResult.Type.BLOCK || !(serverHit instanceof BlockHitResult blockHit)
                || !hitPos.equals(blockHit.getBlockPos()) || hitFace != blockHit.getDirection()
                || blockHit.getLocation().distanceToSqr(target.getHitLocation()) > HIT_LOCATION_EPSILON_SQ) {
            return Optional.empty();
        }

        var hitState = level.getBlockState(hitPos);
        if (hitState.getRenderShape() != RenderShape.MODEL
                || !hitState.getFluidState().isEmpty()
                || !hitState.isSolidRender(level, hitPos)
                || !Block.isShapeFullBlock(hitState.getOcclusionShape(level, hitPos))) {
            return Optional.empty();
        }

        return Optional.of(new ValidatedTarget(hitPos.immutable(), placePos.immutable(), hitState.getBlock()));
    }

    private static boolean hasCompleteFiniteTarget(BlockTargetData target) {
        if (target == null || !target.hasTarget() || target.getHitBlockPos() == null || target.getHitFace() == null
                || target.getPlacePos() == null || target.getPlaceFacing() == null) {
            return false;
        }

        var location = target.getHitLocation();
        return Double.isFinite(location.x) && Double.isFinite(location.y) && Double.isFinite(location.z);
    }

    record ValidatedTarget(BlockPos hitPos, BlockPos placePos, Block targetBlock) {
    }
}
