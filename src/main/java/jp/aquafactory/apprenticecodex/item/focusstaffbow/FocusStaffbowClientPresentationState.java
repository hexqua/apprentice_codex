package jp.aquafactory.apprenticecodex.item.focusstaffbow;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class FocusStaffbowClientPresentationState {
    private static final Map<UUID, PresentationData> PENDING_PRESENTATIONS = new HashMap<>();
    private static final Map<UUID, PresentationData> ACTIVE_PRESENTATIONS = new HashMap<>();

    private FocusStaffbowClientPresentationState() {
    }

    public static void markPending(UUID entityId, String spellId) {
        markPending(entityId, spellId, null);
    }

    public static void markPending(UUID entityId, String spellId, @Nullable CompoundTag data) {
        if (!spellId.isEmpty()) {
            PENDING_PRESENTATIONS.put(entityId, PresentationData.create(spellId, data));
        }
    }

    public static boolean activatePending(UUID entityId, String spellId) {
        var pendingData = PENDING_PRESENTATIONS.get(entityId);
        if (pendingData == null || !pendingData.spellId().equals(spellId)) {
            return false;
        }

        PENDING_PRESENTATIONS.remove(entityId);
        ACTIVE_PRESENTATIONS.put(entityId, pendingData);
        return true;
    }

    public static void clear(UUID entityId) {
        PENDING_PRESENTATIONS.remove(entityId);
        ACTIVE_PRESENTATIONS.remove(entityId);
    }

    public static void clearAll() {
        PENDING_PRESENTATIONS.clear();
        ACTIVE_PRESENTATIONS.clear();
    }

    public static boolean hasPending(UUID entityId, @Nullable String spellId) {
        var pendingData = PENDING_PRESENTATIONS.get(entityId);
        return pendingData != null && (spellId == null || pendingData.spellId().equals(spellId));
    }

    public static boolean hasActive(UUID entityId, @Nullable String spellId) {
        var activeData = ACTIVE_PRESENTATIONS.get(entityId);
        return activeData != null && (spellId == null || activeData.spellId().equals(spellId));
    }

    public static FocusStaffbowChargeEffectState resolveChargeEffectState(UUID entityId) {
        var data = ACTIVE_PRESENTATIONS.get(entityId);
        if (data == null) {
            data = PENDING_PRESENTATIONS.get(entityId);
        }
        if (data == null) {
            return FocusStaffbowChargeEffectState.HIDDEN;
        }

        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return FocusStaffbowChargeEffectState.HIDDEN;
        }

        var elapsedTicks = Math.max(0L, minecraft.level.getGameTime() - data.startedGameTime());
        if ("continuous".equals(data.castMode())) {
            return FocusStaffbowChargeEffectState.visible(
                    data.spellId(),
                    data.startedGameTime(),
                    elapsedTicks,
                    FocusStaffbowChargeLogic.computeContinuousChargeMultiplier(elapsedTicks),
                    1.0F
            );
        }

        var multiplier = elapsedTicks < data.requiredCastTicks()
                ? 1.0D
                : FocusStaffbowChargeLogic.computePendingChargeMultiplier(elapsedTicks, data.chargeBaselineTicks());
        var longRampProgress = data.requiredCastTicks() > 0 && elapsedTicks < data.requiredCastTicks()
                ? Mth.clamp(elapsedTicks / (float) data.requiredCastTicks(), 0.0F, 1.0F)
                : 1.0F;
        return FocusStaffbowChargeEffectState.visible(
                data.spellId(),
                data.startedGameTime(),
                elapsedTicks,
                multiplier,
                longRampProgress
        );
    }

    private record PresentationData(
            String spellId,
            String castMode,
            long startedGameTime,
            int requiredCastTicks,
            int chargeBaselineTicks
    ) {
        private static PresentationData create(String fallbackSpellId, @Nullable CompoundTag data) {
            var minecraft = Minecraft.getInstance();
            var fallbackGameTime = minecraft.level != null ? minecraft.level.getGameTime() : 0L;
            if (data == null || data.isEmpty()) {
                return new PresentationData(fallbackSpellId, "pending", fallbackGameTime, 0, 0);
            }

            var spellId = data.contains("spellId") ? data.getString("spellId") : fallbackSpellId;
            var castMode = data.contains("castMode") ? data.getString("castMode") : "pending";
            var startedGameTime = data.contains("startedGameTime") ? data.getLong("startedGameTime") : fallbackGameTime;
            var requiredCastTicks = data.contains("requiredCastTicks") ? Math.max(0, data.getInt("requiredCastTicks")) : 0;
            var chargeBaselineTicks = data.contains("chargeBaselineTicks")
                    ? Math.max(0, data.getInt("chargeBaselineTicks"))
                    : requiredCastTicks;
            return new PresentationData(spellId, castMode, startedGameTime, requiredCastTicks, chargeBaselineTicks);
        }
    }
}
