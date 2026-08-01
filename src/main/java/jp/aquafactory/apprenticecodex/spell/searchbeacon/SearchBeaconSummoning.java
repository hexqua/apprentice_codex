package jp.aquafactory.apprenticecodex.spell.searchbeacon;

import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class SearchBeaconSummoning {
    private static final AABB ACTIVE_BEACON_CHECK_BOX =
            new AABB(-3.0E7, -2048.0, -3.0E7, 3.0E7, 2048.0, 3.0E7);

    private SearchBeaconSummoning() {
    }

    public static Failure validate(ServerLevel level, ServerPlayer owner) {
        if (hasActiveBeacon(level, owner.getUUID())) {
            return Failure.ALREADY_ACTIVE;
        }
        return resolveSummonPosition(level, owner) == null ? Failure.CANNOT_PLACE : Failure.NONE;
    }

    public static @Nullable SearchBeaconEntity summon(
            ServerLevel level,
            ServerPlayer owner,
            int initialRange,
            int additionalRangePerItem,
            ItemStack preSearchRefund
    ) {
        var summonPosition = resolveSummonPosition(level, owner);
        if (summonPosition == null) {
            return null;
        }

        var beacon = new SearchBeaconEntity(EntityRegistry.SEARCH_BEACON.get(), level);
        beacon.setOwner(owner);
        beacon.setAnchor(summonPosition);
        beacon.setSearchTuning(initialRange, additionalRangePerItem);
        beacon.setPreSearchRefund(preSearchRefund);
        beacon.moveTo(summonPosition.x, summonPosition.y, summonPosition.z, owner.getYRot(), 0.0F);
        return level.addFreshEntity(beacon) ? beacon : null;
    }

    private static boolean hasActiveBeacon(ServerLevel level, UUID ownerId) {
        return !level.getEntitiesOfClass(
                SearchBeaconEntity.class,
                ACTIVE_BEACON_CHECK_BOX,
                beacon -> beacon.isAlive() && beacon.isOwnedBy(ownerId)
        ).isEmpty();
    }

    private static @Nullable Vec3 resolveSummonPosition(ServerLevel level, ServerPlayer owner) {
        var hit = owner.pick(12.0, 1.0F, false);
        Vec3 desired = switch (hit.getType()) {
            case BLOCK -> {
                var blockHit = (BlockHitResult) hit;
                yield blockHit.getLocation().add(
                        blockHit.getDirection().getStepX() * 0.35,
                        Math.max(0, blockHit.getDirection().getStepY()) * 0.35 + 0.05,
                        blockHit.getDirection().getStepZ() * 0.35
                );
            }
            case ENTITY, MISS -> owner.getEyePosition().add(owner.getLookAngle().scale(4.5));
        };

        var halfWidth = SearchBeaconEntity.WIDTH / 2.0;
        for (int i = 0; i < 5; i++) {
            var y = desired.y + i * 0.25;
            var box = new AABB(
                    desired.x - halfWidth,
                    y,
                    desired.z - halfWidth,
                    desired.x + halfWidth,
                    y + SearchBeaconEntity.HEIGHT,
                    desired.z + halfWidth
            );
            if (level.noCollision(null, box)
                    && level.getEntities(owner, box, EntitySelector.NO_SPECTATORS).isEmpty()) {
                return new Vec3(desired.x, y, desired.z);
            }
        }

        return null;
    }

    public enum Failure {
        NONE,
        ALREADY_ACTIVE,
        CANNOT_PLACE
    }
}
