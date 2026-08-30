package jp.aquafactory.apprenticecodex.spell.archermultiple;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;

public final class ArcherMultipleManager {
    private ArcherMultipleManager() {
    }

    public enum ValidationResult {
        ACTIVE,
        EXPIRED,
        INVALID
    }

    public static void initialize(ServerPlayer owner, ArcherMultipleBowEntity bow, int duration) {
        bow.setLifecycleOwner(owner);
        bow.setExpirationGameTime(bow.level().getGameTime() + duration);
    }

    public static ValidationResult validate(ArcherMultipleBowEntity bow) {
        if (!bow.hasExpirationGameTime()) return ValidationResult.INVALID;
        if (bow.level().getGameTime() >= bow.getExpirationGameTime()) return ValidationResult.EXPIRED;
        var owner = bow.resolvePlayerOwner();
        if (owner == null || !owner.isAlive() || owner.level() != bow.level()) return ValidationResult.INVALID;
        var recast = getActiveRecast(owner);
        return recast != null
                && recast.getCastData() instanceof ArcherMultiple.ArcherMultipleCastData castData
                && castData.matches(bow)
                ? ValidationResult.ACTIVE
                : ValidationResult.INVALID;
    }

    public static void finishRecast(ServerPlayer player, ICastDataSerializable rawCastData) {
        if (rawCastData instanceof ArcherMultiple.ArcherMultipleCastData castData) {
            discardLoadedBows(player, castData);
        }
        // 旧SummonManager形式や不整合データでも、ownerに属する残留弓は復元せず終了させる。
        discardOwnedBows(player);
    }

    public static void cancel(ServerPlayer player, RecastResult result) {
        var recast = getActiveRecast(player);
        if (recast != null) {
            MagicData.getPlayerMagicData(player).getPlayerRecasts().removeRecast(recast, result);
        } else {
            discardOwnedBows(player);
        }
    }

    public static void expire(ArcherMultipleBowEntity bow) {
        var owner = bow.resolvePlayerOwner();
        var recast = owner == null ? null : getMatchingRecast(owner, bow);
        if (recast != null) {
            MagicData.getPlayerMagicData(owner).getPlayerRecasts().removeRecast(recast, RecastResult.TIMEOUT);
        } else {
            bow.discardForLifecycle();
        }
    }

    public static void onDestroyed(ArcherMultipleBowEntity bow) {
        var owner = bow.resolvePlayerOwner();
        var recast = owner == null ? null : getMatchingRecast(owner, bow);
        if (recast == null || !(recast.getCastData() instanceof ArcherMultiple.ArcherMultipleCastData castData)) {
            return;
        }
        if (castData.removeBow(bow.getUUID())) {
            MagicData.getPlayerMagicData(owner).getPlayerRecasts()
                    .removeRecast(recast, RecastResult.USED_ALL_RECASTS);
        }
    }

    private static RecastInstance getActiveRecast(ServerPlayer player) {
        var recasts = MagicData.getPlayerMagicData(player).getPlayerRecasts();
        var recast = recasts.getRecastInstance(SpellRegistry.ARCHER_MULTIPLE.get().getSpellId());
        return recasts.isRecastActive(recast) ? recast : null;
    }

    private static RecastInstance getMatchingRecast(ServerPlayer player, ArcherMultipleBowEntity bow) {
        var recast = getActiveRecast(player);
        return recast != null
                && recast.getCastData() instanceof ArcherMultiple.ArcherMultipleCastData castData
                && castData.matches(bow)
                ? recast
                : null;
    }

    private static void discardLoadedBows(ServerPlayer player, ArcherMultiple.ArcherMultipleCastData castData) {
        if (castData.getDimension() == null || castData.getBowUuids().isEmpty()) return;
        for (var level : player.server.getAllLevels()) {
            if (!level.dimension().location().equals(castData.getDimension())) continue;
            for (var uuid : castData.getBowUuids()) {
                var entity = level.getEntity(uuid);
                if (entity instanceof ArcherMultipleBowEntity bow && bow.isOwnedBy(player)) {
                    bow.discardForLifecycle();
                }
            }
            return;
        }
    }

    private static void discardOwnedBows(ServerPlayer player) {
        var owned = new ArrayList<ArcherMultipleBowEntity>();
        for (var level : player.server.getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (entity instanceof ArcherMultipleBowEntity bow && !bow.isRemoved() && bow.isOwnedBy(player)) {
                    owned.add(bow);
                }
            }
        }
        owned.forEach(ArcherMultipleBowEntity::discardForLifecycle);
    }
}
