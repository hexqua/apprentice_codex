package jp.aquafactory.apprenticecodex.spell.servantgaze;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public final class ServantGazeManager {
    private ServantGazeManager() {
    }

    public enum ValidationResult {
        ACTIVE,
        EXPIRED,
        INVALID
    }

    public static void initialize(ServerPlayer owner, ServantGazeStaffEntity staff, int duration,
                                  ServantGaze.ServantGazeCastData castData) {
        staff.setLifecycleOwner(owner);
        staff.setExpirationGameTime(staff.level().getGameTime() + duration);
        configure(staff, castData);
        castData.bindStaff(staff);
    }

    public static ValidationResult validate(ServantGazeStaffEntity staff) {
        if (!staff.hasExpirationGameTime()) return ValidationResult.INVALID;
        if (staff.level().getGameTime() >= staff.getExpirationGameTime()) return ValidationResult.EXPIRED;
        var owner = staff.resolvePlayerOwner();
        if (owner == null || !owner.isAlive() || owner.level() != staff.level()) return ValidationResult.INVALID;
        var recast = getActiveRecast(owner);
        return recast != null
                && recast.getCastData() instanceof ServantGaze.ServantGazeCastData castData
                && castData.matches(staff)
                ? ValidationResult.ACTIVE
                : ValidationResult.INVALID;
    }

    public static void finishRecast(ServerPlayer player, ICastDataSerializable rawCastData) {
        if (rawCastData instanceof ServantGaze.ServantGazeCastData castData) {
            discardLoadedStaff(player, castData);
        }
        // 旧toggle形式や不整合データでも、ownerに属する残留杖は復元せず終了させる。
        discardOwnedStaffs(player);
    }

    public static void cancel(ServerPlayer player, RecastResult result) {
        var recast = getActiveRecast(player);
        if (recast != null) {
            MagicData.getPlayerMagicData(player).getPlayerRecasts().removeRecast(recast, result);
        } else {
            discardOwnedStaffs(player);
        }
    }

    public static void expire(ServantGazeStaffEntity staff) {
        var owner = staff.resolvePlayerOwner();
        var recast = owner == null ? null : getMatchingRecast(owner, staff);
        if (recast != null) {
            MagicData.getPlayerMagicData(owner).getPlayerRecasts().removeRecast(recast, RecastResult.TIMEOUT);
        } else {
            staff.discardForLifecycle();
        }
    }

    public static void onDestroyed(ServantGazeStaffEntity staff) {
        var owner = staff.resolvePlayerOwner();
        var recast = owner == null ? null : getMatchingRecast(owner, staff);
        if (recast != null) {
            MagicData.getPlayerMagicData(owner).getPlayerRecasts()
                    .removeRecast(recast, RecastResult.USED_ALL_RECASTS);
        }
    }

    public static void relocate(ServerPlayer player) {
        var recast = getActiveRecast(player);
        if (recast == null || !(recast.getCastData() instanceof ServantGaze.ServantGazeCastData castData)) {
            discardOwnedStaffs(player);
            return;
        }

        discardOwnedStaffs(player);
        var staff = new ServantGazeStaffEntity(EntityRegistry.SERVANT_GAZE_STAFF.get(), player.serverLevel(), player);
        initialize(player, staff, Math.max(1, recast.getTicksRemaining()), castData);
        if (!player.serverLevel().addFreshEntity(staff)) {
            MagicData.getPlayerMagicData(player).getPlayerRecasts().removeRecast(recast, RecastResult.USER_CANCEL);
        }
    }

    private static void configure(ServantGazeStaffEntity staff, ServantGaze.ServantGazeCastData castData) {
        staff.configure(castData.getSpellLevel(), castData.getDamage(), castData.getRadius(),
                castData.getAttackManaCost());
    }

    private static @Nullable RecastInstance getActiveRecast(ServerPlayer player) {
        var recasts = MagicData.getPlayerMagicData(player).getPlayerRecasts();
        var recast = recasts.getRecastInstance(SpellRegistry.SERVANT_GAZE.get().getSpellId());
        return recasts.isRecastActive(recast) ? recast : null;
    }

    private static @Nullable RecastInstance getMatchingRecast(ServerPlayer player, ServantGazeStaffEntity staff) {
        var recast = getActiveRecast(player);
        return recast != null
                && recast.getCastData() instanceof ServantGaze.ServantGazeCastData castData
                && castData.matches(staff)
                ? recast
                : null;
    }

    private static void discardLoadedStaff(ServerPlayer player, ServantGaze.ServantGazeCastData castData) {
        if (castData.getStaffUuid() == null || castData.getDimension() == null) return;
        for (var level : player.server.getAllLevels()) {
            if (!level.dimension().location().equals(castData.getDimension())) continue;
            var entity = level.getEntity(castData.getStaffUuid());
            if (entity instanceof ServantGazeStaffEntity staff && staff.isOwnedBy(player)) {
                staff.discardForLifecycle();
            }
            return;
        }
    }

    private static void discardOwnedStaffs(ServerPlayer player) {
        var owned = new ArrayList<ServantGazeStaffEntity>();
        for (var level : player.server.getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (entity instanceof ServantGazeStaffEntity staff && !staff.isRemoved() && staff.isOwnedBy(player)) {
                    owned.add(staff);
                }
            }
        }
        owned.forEach(ServantGazeStaffEntity::discardForLifecycle);
    }
}
