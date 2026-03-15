package jp.aquafactory.apprenticecodex.utility;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BlockTargetingHelper {
    private static final double RANGE_EPSILON = 0.25;
    // 右クリック入力との同期に必要な最小限だけ保持し、無関係な後続キャストへの混入を抑える。
    private static final long PENDING_TARGET_EXPIRE_TICKS = 3L;
    private static final ConcurrentHashMap<UUID, PendingTarget> PENDING_SERVER_TARGETS = new ConcurrentHashMap<>();

    private BlockTargetingHelper() {
    }

    public static void setPendingServerTarget(ServerPlayer serverPlayer, ResourceLocation spellId, @Nullable BlockTargetData targetData) {
        if (targetData == null || !targetData.hasTarget()) {
            clearPendingServerTarget(serverPlayer);
            return;
        }

        PENDING_SERVER_TARGETS.put(serverPlayer.getUUID(), new PendingTarget(
                spellId,
                targetData.copy(),
                serverPlayer.level().getGameTime() + PENDING_TARGET_EXPIRE_TICKS
        ));
    }

    public static void clearPendingServerTarget(ServerPlayer serverPlayer) {
        PENDING_SERVER_TARGETS.remove(serverPlayer.getUUID());
    }

    public static Optional<BlockTargetData> getValidatedPendingTarget(Level level, LivingEntity entity, ResourceLocation expectedSpellId, double range) {
        if (!(entity instanceof ServerPlayer serverPlayer)) {
            return Optional.empty();
        }

        var pendingTarget = PENDING_SERVER_TARGETS.get(serverPlayer.getUUID());
        if (pendingTarget == null) {
            return Optional.empty();
        }
        if (level.getGameTime() > pendingTarget.expireGameTime()) {
            PENDING_SERVER_TARGETS.remove(serverPlayer.getUUID());
            return Optional.empty();
        }
        if (!pendingTarget.spellId().equals(expectedSpellId)) {
            PENDING_SERVER_TARGETS.remove(serverPlayer.getUUID());
            return Optional.empty();
        }

        var validated = validateTarget(level, entity, range, pendingTarget.targetData());
        validated.ifPresent(unused -> PENDING_SERVER_TARGETS.remove(serverPlayer.getUUID()));
        return validated;
    }

    public static Optional<BlockTools.PlaceData> findClientPlacePos(Level level, LivingEntity entity, ResourceLocation expectedSpellId, double range) {
        return getValidatedPendingTarget(level, entity, expectedSpellId, range)
                .map(target -> new BlockTools.PlaceData(target.getPlacePos(), target.getPlaceFacing()));
    }

    public static Optional<BlockTargetData> validateTarget(Level level, LivingEntity entity, double range, @Nullable BlockTargetData targetData) {
        if (targetData == null || !targetData.hasTarget()) {
            return Optional.empty();
        }
        if (targetData.getHitBlockPos() == null || targetData.getHitFace() == null
                || targetData.getPlacePos() == null || targetData.getPlaceFacing() == null) {
            return Optional.empty();
        }

        var allowedRange = range + RANGE_EPSILON;
        var distanceSq = entity.getEyePosition(1.0F).distanceToSqr(targetData.getHitLocation());
        if (distanceSq > allowedRange * allowedRange) {
            return Optional.empty();
        }

        var hitPos = targetData.getHitBlockPos();
        var hitFace = targetData.getHitFace();
        var expectedPlacePos = level.getBlockState(hitPos).canBeReplaced() ? hitPos : hitPos.relative(hitFace);
        if (!expectedPlacePos.equals(targetData.getPlacePos())) {
            return Optional.empty();
        }
        if (targetData.getPlaceFacing() != hitFace.getOpposite()) {
            return Optional.empty();
        }
        if (!level.getBlockState(targetData.getPlacePos()).canBeReplaced()) {
            return Optional.empty();
        }

        return Optional.of(targetData.copy());
    }

    private record PendingTarget(ResourceLocation spellId, BlockTargetData targetData, long expireGameTime) {
    }
}
