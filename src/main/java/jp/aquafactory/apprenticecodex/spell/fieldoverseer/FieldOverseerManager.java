package jp.aquafactory.apprenticecodex.spell.fieldoverseer;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public final class FieldOverseerManager {
    private FieldOverseerManager() {
    }

    public enum ValidationResult {
        ACTIVE,
        EXPIRED,
        INVALID
    }

    public static void initialize(LivingEntity owner, FieldOverseerStaffEntity staff, BlockPos position, int duration,
                                  FieldOverseer.FieldOverseerCastData castData) {
        staff.setOwner(owner);
        staff.setExpirationGameTime(staff.level().getGameTime() + duration);
        castData.bindStaff(staff, position);
    }

    public static ValidationResult validate(FieldOverseerStaffEntity staff) {
        if (!staff.hasExpirationGameTime()) {
            return ValidationResult.INVALID;
        }
        if (staff.level().getGameTime() >= staff.getExpirationGameTime()) {
            return ValidationResult.EXPIRED;
        }
        if (!staff.isPlayerOwned()) {
            return ValidationResult.ACTIVE;
        }
        var owner = staff.resolvePlayerOwner();
        if (owner == null || owner.level() != staff.level()) {
            return ValidationResult.INVALID;
        }
        var recast = getActiveRecast(owner);
        return recast != null
                && recast.getCastData() instanceof FieldOverseer.FieldOverseerCastData castData
                && castData.matches(staff)
                ? ValidationResult.ACTIVE
                : ValidationResult.INVALID;
    }

    public static boolean finishRecast(ServerPlayer player, RecastInstance recast, RecastResult result,
                                       ICastDataSerializable rawCastData) {
        if (result == RecastResult.COUNTERSPELL) {
            // Iron's の既存召喚と同様、カウンタースペルでは設置済みの杖と再詠唱を維持する。
            MagicData.getPlayerMagicData(player).getPlayerRecasts().forceAddRecast(recast);
        } else if (rawCastData instanceof FieldOverseer.FieldOverseerCastData castData) {
            discardLoadedStaff(player, castData);
        }
        return result != RecastResult.TIMEOUT
                || !ItemRegistry.GREATER_CONJURERS_TALISMAN.get().isEquippedBy(player);
    }

    public static void cancel(ServerPlayer player) {
        var recast = getActiveRecast(player);
        if (recast != null) {
            MagicData.getPlayerMagicData(player).getPlayerRecasts()
                    .removeRecast(recast, RecastResult.USER_CANCEL);
        }
    }

    public static void expire(FieldOverseerStaffEntity staff) {
        var owner = staff.resolvePlayerOwner();
        var recast = owner == null ? null : getMatchingRecast(owner, staff);
        if (recast != null) {
            MagicData.getPlayerMagicData(owner).getPlayerRecasts().removeRecast(recast, RecastResult.TIMEOUT);
        } else {
            staff.discardForLifecycle();
        }
    }

    public static void onDestroyed(FieldOverseerStaffEntity staff) {
        var owner = staff.resolvePlayerOwner();
        var recast = owner == null ? null : getMatchingRecast(owner, staff);
        if (recast != null) {
            MagicData.getPlayerMagicData(owner).getPlayerRecasts()
                    .removeRecast(recast, RecastResult.USED_ALL_RECASTS);
        }
    }

    private static @Nullable RecastInstance getActiveRecast(ServerPlayer player) {
        var recasts = MagicData.getPlayerMagicData(player).getPlayerRecasts();
        var recast = recasts.getRecastInstance(SpellRegistry.FIELD_OVERSEER.get().getSpellId());
        return recasts.isRecastActive(recast) ? recast : null;
    }

    private static @Nullable RecastInstance getMatchingRecast(ServerPlayer player,
                                                               FieldOverseerStaffEntity staff) {
        var recast = getActiveRecast(player);
        return recast != null
                && recast.getCastData() instanceof FieldOverseer.FieldOverseerCastData castData
                && castData.matches(staff)
                ? recast
                : null;
    }

    private static void discardLoadedStaff(ServerPlayer player, FieldOverseer.FieldOverseerCastData castData) {
        if (castData.getStaffUuid() == null || castData.getDimension() == null) {
            return;
        }
        for (var level : player.server.getAllLevels()) {
            if (!level.dimension().location().equals(castData.getDimension())) {
                continue;
            }
            var entity = level.getEntity(castData.getStaffUuid());
            if (entity instanceof FieldOverseerStaffEntity staff && staff.isOwnedBy(player)) {
                staff.discardForLifecycle();
            }
            return;
        }
    }
}
