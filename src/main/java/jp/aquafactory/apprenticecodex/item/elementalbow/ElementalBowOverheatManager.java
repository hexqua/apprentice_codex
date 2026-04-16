package jp.aquafactory.apprenticecodex.item.elementalbow;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ElementalBowOverheatManager {
    private static final String ROOT_TAG = "ApprenticeCodexElementalBowOverheat";
    private static final String OBSERVED_ROOT_TAG = "ApprenticeCodexElementalBowOverheatObserved";
    private static final String EXPIRE_GAME_TIME_TAG = "ExpireGameTime";
    private static final String CHAIN_DEPTH_TAG = "ChainDepth";
    private static final String PENDING_COOLDOWN_TICKS_TAG = "PendingCooldownTicks";
    private static final float EXTRA_MANA_LINEAR_MULTIPLIER = 0.20F;
    private static final float EXTRA_MANA_QUADRATIC_MULTIPLIER = 0.08F;

    private ElementalBowOverheatManager() {
    }

    public static float getAdditionalManaCost(@NotNull Player player, @Nullable ResourceLocation schoolId, float baseManaCost) {
        if (schoolId == null || baseManaCost <= 0.0F) {
            return 0.0F;
        }

        var state = getState(player, schoolId);
        if (!state.active()) {
            return 0.0F;
        }

        var step = state.chainDepth();
        var multiplier = EXTRA_MANA_LINEAR_MULTIPLIER * step + EXTRA_MANA_QUADRATIC_MULTIPLIER * step * step;
        return baseManaCost * multiplier;
    }

    public static void storePendingCooldown(@NotNull Player player, @Nullable ResourceLocation schoolId, int cooldownTicks) {
        if (schoolId == null) {
            return;
        }

        if (cooldownTicks <= 0) {
            clearPendingCooldown(player, schoolId);
            return;
        }

        var tag = getSchoolTag(player, schoolId, true);
        if (tag != null) {
            tag.putInt(PENDING_COOLDOWN_TICKS_TAG, cooldownTicks);
        }
    }

    public static int consumePendingCooldown(@NotNull Player player, @Nullable ResourceLocation schoolId, int fallbackCooldownTicks) {
        if (schoolId == null) {
            return Math.max(0, fallbackCooldownTicks);
        }

        var schoolTag = getSchoolTag(player, schoolId, false);
        if (schoolTag == null || !schoolTag.contains(PENDING_COOLDOWN_TICKS_TAG, Tag.TAG_INT)) {
            return Math.max(0, fallbackCooldownTicks);
        }

        var cooldownTicks = Math.max(0, schoolTag.getInt(PENDING_COOLDOWN_TICKS_TAG));
        schoolTag.remove(PENDING_COOLDOWN_TICKS_TAG);
        pruneSchoolTag(player, schoolId, schoolTag);
        return cooldownTicks > 0 ? cooldownTicks : Math.max(0, fallbackCooldownTicks);
    }

    public static void applyOverheatAfterCast(@NotNull Player player, @Nullable ResourceLocation schoolId, int cooldownTicks) {
        if (schoolId == null) {
            return;
        }

        if (cooldownTicks <= 0) {
            clear(player, schoolId);
            return;
        }

        var state = getState(player, schoolId);
        var nextChainDepth = state.active() ? state.chainDepth() + 1 : 1;
        var schoolTag = getSchoolTag(player, schoolId, true);
        if (schoolTag != null) {
            schoolTag.putLong(EXPIRE_GAME_TIME_TAG, player.level().getGameTime() + cooldownTicks);
            schoolTag.putInt(CHAIN_DEPTH_TAG, nextChainDepth);
            schoolTag.remove(PENDING_COOLDOWN_TICKS_TAG);
        }
        syncToClientIfNeeded(player);
    }

    public static OverheatState getState(@NotNull Player player, @Nullable ResourceLocation schoolId) {
        if (schoolId == null) {
            return OverheatState.INACTIVE;
        }

        var rootTag = getRootTag(player, false);
        if (rootTag == null || !rootTag.contains(schoolId.toString(), Tag.TAG_COMPOUND)) {
            return OverheatState.INACTIVE;
        }

        var schoolTag = rootTag.getCompound(schoolId.toString());
        var expireGameTime = schoolTag.getLong(EXPIRE_GAME_TIME_TAG);
        var chainDepth = Math.max(0, schoolTag.getInt(CHAIN_DEPTH_TAG));
        if (chainDepth == 0 || expireGameTime <= player.level().getGameTime()) {
            rootTag.remove(schoolId.toString());
            if (rootTag.isEmpty()) {
                player.getPersistentData().remove(ROOT_TAG);
            }
            return OverheatState.INACTIVE;
        }

        return new OverheatState(chainDepth, expireGameTime);
    }

    public static void clear(@NotNull Player player, @Nullable ResourceLocation schoolId) {
        if (schoolId == null) {
            return;
        }

        var rootTag = getRootTag(player, false);
        if (rootTag == null) {
            return;
        }

        rootTag.remove(schoolId.toString());
        if (rootTag.isEmpty()) {
            player.getPersistentData().remove(ROOT_TAG);
        }
        syncToClientIfNeeded(player);
    }

    public static void clearPendingCooldown(@NotNull Player player, @Nullable ResourceLocation schoolId) {
        if (schoolId == null) {
            return;
        }

        var schoolTag = getSchoolTag(player, schoolId, false);
        if (schoolTag == null) {
            return;
        }

        schoolTag.remove(PENDING_COOLDOWN_TICKS_TAG);
        pruneSchoolTag(player, schoolId, schoolTag);
    }

    public static @NotNull List<ResourceLocation> collectCooledSchoolsWhileHolding(@NotNull Player player) {
        var cooledSchools = new ArrayList<ResourceLocation>();
        var observedRootTag = getObservedRootTag(player, false);
        if (observedRootTag != null) {
            for (var schoolKey : List.copyOf(observedRootTag.getAllKeys())) {
                var schoolId = ResourceLocation.tryParse(schoolKey);
                if (schoolId == null) {
                    observedRootTag.remove(schoolKey);
                    continue;
                }

                if (!getState(player, schoolId).active()) {
                    cooledSchools.add(schoolId);
                    observedRootTag.remove(schoolKey);
                }
            }
            pruneObservedRootTag(player, observedRootTag);
        }

        var rootTag = getRootTag(player, false);
        if (rootTag == null) {
            return cooledSchools;
        }

        var refreshedObservedRootTag = getObservedRootTag(player, true);
        for (var schoolKey : List.copyOf(rootTag.getAllKeys())) {
            var schoolId = ResourceLocation.tryParse(schoolKey);
            if (schoolId == null) {
                rootTag.remove(schoolKey);
                continue;
            }

            if (getState(player, schoolId).active()) {
                if (refreshedObservedRootTag != null) {
                    refreshedObservedRootTag.putBoolean(schoolKey, true);
                }
            }
        }
        if (refreshedObservedRootTag != null) {
            pruneObservedRootTag(player, refreshedObservedRootTag);
        }
        return cooledSchools;
    }

    public static void clearObservedSchools(@NotNull Player player) {
        player.getPersistentData().remove(OBSERVED_ROOT_TAG);
    }

    public static @NotNull CompoundTag createSyncTag(@NotNull Player player) {
        var syncTag = new CompoundTag();
        var rootTag = getRootTag(player, false);
        if (rootTag == null) {
            return syncTag;
        }

        long gameTime = player.level().getGameTime();
        for (var schoolKey : List.copyOf(rootTag.getAllKeys())) {
            var schoolId = ResourceLocation.tryParse(schoolKey);
            if (schoolId == null) {
                rootTag.remove(schoolKey);
                continue;
            }

            var schoolTag = rootTag.getCompound(schoolKey);
            var expireGameTime = schoolTag.getLong(EXPIRE_GAME_TIME_TAG);
            var chainDepth = Math.max(0, schoolTag.getInt(CHAIN_DEPTH_TAG));
            if (chainDepth == 0 || expireGameTime <= gameTime) {
                rootTag.remove(schoolKey);
                continue;
            }

            var syncedSchoolTag = new CompoundTag();
            syncedSchoolTag.putLong(EXPIRE_GAME_TIME_TAG, expireGameTime);
            syncedSchoolTag.putInt(CHAIN_DEPTH_TAG, chainDepth);
            syncTag.put(schoolKey, syncedSchoolTag);
        }

        if (rootTag.isEmpty()) {
            player.getPersistentData().remove(ROOT_TAG);
        }
        return syncTag;
    }

    public static void applySyncedState(@NotNull Player player, @Nullable CompoundTag syncedRootTag) {
        if (syncedRootTag == null || syncedRootTag.isEmpty()) {
            player.getPersistentData().remove(ROOT_TAG);
            return;
        }

        player.getPersistentData().put(ROOT_TAG, syncedRootTag.copy());
    }

    @Nullable
    private static CompoundTag getRootTag(Player player, boolean create) {
        var persistentData = player.getPersistentData();
        if (!persistentData.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            if (!create) {
                return null;
            }

            var rootTag = new CompoundTag();
            persistentData.put(ROOT_TAG, rootTag);
            return rootTag;
        }

        return persistentData.getCompound(ROOT_TAG);
    }

    @Nullable
    private static CompoundTag getObservedRootTag(Player player, boolean create) {
        var persistentData = player.getPersistentData();
        if (!persistentData.contains(OBSERVED_ROOT_TAG, Tag.TAG_COMPOUND)) {
            if (!create) {
                return null;
            }

            var rootTag = new CompoundTag();
            persistentData.put(OBSERVED_ROOT_TAG, rootTag);
            return rootTag;
        }

        return persistentData.getCompound(OBSERVED_ROOT_TAG);
    }

    @Nullable
    private static CompoundTag getSchoolTag(Player player, ResourceLocation schoolId, boolean create) {
        var rootTag = getRootTag(player, create);
        if (rootTag == null) {
            return null;
        }

        var schoolKey = schoolId.toString();
        if (!rootTag.contains(schoolKey, Tag.TAG_COMPOUND)) {
            if (!create) {
                return null;
            }

            var schoolTag = new CompoundTag();
            rootTag.put(schoolKey, schoolTag);
            return schoolTag;
        }

        return rootTag.getCompound(schoolKey);
    }

    private static void pruneSchoolTag(Player player, ResourceLocation schoolId, CompoundTag schoolTag) {
        if (!schoolTag.isEmpty()) {
            return;
        }

        var rootTag = getRootTag(player, false);
        if (rootTag == null) {
            return;
        }

        rootTag.remove(schoolId.toString());
        if (rootTag.isEmpty()) {
            player.getPersistentData().remove(ROOT_TAG);
        }
    }

    private static void pruneObservedRootTag(Player player, CompoundTag observedRootTag) {
        if (!observedRootTag.isEmpty()) {
            return;
        }

        player.getPersistentData().remove(OBSERVED_ROOT_TAG);
    }

    private static void syncToClientIfNeeded(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            // player persistentData は自動同期されないため、描画用の overheat 状態は明示的に client へ送る。
            ElementalBowOverheatSync.syncToClient(serverPlayer);
        }
    }

    public record OverheatState(int chainDepth, long expireGameTime) {
        private static final OverheatState INACTIVE = new OverheatState(0, 0L);

        public boolean active() {
            return chainDepth > 0;
        }
    }
}
