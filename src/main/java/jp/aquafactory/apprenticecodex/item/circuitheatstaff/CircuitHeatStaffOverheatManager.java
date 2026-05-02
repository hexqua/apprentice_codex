package jp.aquafactory.apprenticecodex.item.circuitheatstaff;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class CircuitHeatStaffOverheatManager {
    private static final String ROOT_TAG = "ApprenticeCodexCircuitHeatStaffOverheat";
    private static final String EXPIRE_GAME_TIME_TAG = "ExpireGameTime";
    private static final String CHAIN_DEPTH_TAG = "ChainDepth";
    private static final String LAST_APPLIED_COOLDOWN_TICKS_TAG = "LastAppliedCooldownTicks";
    private static final float EXTRA_MANA_LINEAR_MULTIPLIER = 0.10F;
    private static final float EXTRA_MANA_QUADRATIC_MULTIPLIER = 0.10F;

    private CircuitHeatStaffOverheatManager() {
    }

    public static int getNextStep(@NotNull Player player, @Nullable String spellId) {
        var state = getState(player, spellId);
        return state.active() ? state.chainDepth() + 1 : 1;
    }

    public static int getAdditionalManaCost(int baseManaCost, int step) {
        if (baseManaCost <= 0 || step <= 0) {
            return 0;
        }

        var multiplier = EXTRA_MANA_LINEAR_MULTIPLIER * step + EXTRA_MANA_QUADRATIC_MULTIPLIER * step * step;
        return Math.max(1, (int)Math.ceil(baseManaCost * multiplier));
    }

    public static void applyAfterBypass(@NotNull Player player, @Nullable String spellId, int cooldownTicks) {
        if (spellId == null || spellId.isBlank() || cooldownTicks <= 0) {
            return;
        }

        var state = getState(player, spellId);
        var nextChainDepth = state.active() ? state.chainDepth() + 1 : 1;
        var spellTag = getSpellTag(player, spellId, true);
        if (spellTag != null) {
            spellTag.putLong(EXPIRE_GAME_TIME_TAG, player.level().getGameTime() + cooldownTicks);
            spellTag.putInt(CHAIN_DEPTH_TAG, nextChainDepth);
            spellTag.putInt(LAST_APPLIED_COOLDOWN_TICKS_TAG, cooldownTicks);
        }
        syncToClientIfNeeded(player);
    }

    public static OverheatState getState(@NotNull Player player, @Nullable String spellId) {
        if (spellId == null || spellId.isBlank()) {
            return OverheatState.INACTIVE;
        }

        var rootTag = getRootTag(player, false);
        if (rootTag == null || !rootTag.contains(spellId, Tag.TAG_COMPOUND)) {
            return OverheatState.INACTIVE;
        }

        var spellTag = rootTag.getCompound(spellId);
        var expireGameTime = spellTag.getLong(EXPIRE_GAME_TIME_TAG);
        var chainDepth = Math.max(0, spellTag.getInt(CHAIN_DEPTH_TAG));
        if (chainDepth == 0 || expireGameTime <= player.level().getGameTime()) {
            rootTag.remove(spellId);
            if (rootTag.isEmpty()) {
                player.getPersistentData().remove(ROOT_TAG);
            }
            syncToClientIfNeeded(player);
            return OverheatState.INACTIVE;
        }

        return new OverheatState(chainDepth, expireGameTime);
    }

    public static boolean hasAnyActiveState(@NotNull Player player) {
        var rootTag = getRootTag(player, false);
        if (rootTag == null) {
            return false;
        }

        for (var spellId : List.copyOf(rootTag.getAllKeys())) {
            if (getState(player, spellId).active()) {
                return true;
            }
        }
        return false;
    }

    public static void clear(@NotNull Player player, @Nullable String spellId) {
        if (spellId == null || spellId.isBlank()) {
            return;
        }

        var rootTag = getRootTag(player, false);
        if (rootTag == null) {
            return;
        }

        rootTag.remove(spellId);
        if (rootTag.isEmpty()) {
            player.getPersistentData().remove(ROOT_TAG);
        }
        syncToClientIfNeeded(player);
    }

    public static @NotNull CompoundTag createSyncTag(@NotNull Player player) {
        var syncTag = new CompoundTag();
        var rootTag = getRootTag(player, false);
        if (rootTag == null) {
            return syncTag;
        }

        for (var spellId : List.copyOf(rootTag.getAllKeys())) {
            var state = getState(player, spellId);
            if (!state.active()) {
                continue;
            }

            var spellTag = rootTag.getCompound(spellId);
            var syncedSpellTag = new CompoundTag();
            syncedSpellTag.putLong(EXPIRE_GAME_TIME_TAG, state.expireGameTime());
            syncedSpellTag.putInt(CHAIN_DEPTH_TAG, state.chainDepth());
            syncedSpellTag.putInt(LAST_APPLIED_COOLDOWN_TICKS_TAG, Math.max(0, spellTag.getInt(LAST_APPLIED_COOLDOWN_TICKS_TAG)));
            syncTag.put(spellId, syncedSpellTag);
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
    private static CompoundTag getSpellTag(Player player, String spellId, boolean create) {
        var rootTag = getRootTag(player, create);
        if (rootTag == null) {
            return null;
        }

        if (!rootTag.contains(spellId, Tag.TAG_COMPOUND)) {
            if (!create) {
                return null;
            }

            var spellTag = new CompoundTag();
            rootTag.put(spellId, spellTag);
            return spellTag;
        }

        return rootTag.getCompound(spellId);
    }

    private static void syncToClientIfNeeded(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            CircuitHeatStaffOverheatSync.syncToClient(serverPlayer);
        }
    }

    public record OverheatState(int chainDepth, long expireGameTime) {
        private static final OverheatState INACTIVE = new OverheatState(0, 0L);

        public boolean active() {
            return chainDepth > 0;
        }
    }
}
