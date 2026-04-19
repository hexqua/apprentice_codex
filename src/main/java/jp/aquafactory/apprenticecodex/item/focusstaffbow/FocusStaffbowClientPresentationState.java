package jp.aquafactory.apprenticecodex.item.focusstaffbow;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class FocusStaffbowClientPresentationState {
    private static final Map<UUID, String> PENDING_PRESENTATIONS = new HashMap<>();
    private static final Map<UUID, String> ACTIVE_PRESENTATIONS = new HashMap<>();

    private FocusStaffbowClientPresentationState() {
    }

    public static void markPending(UUID entityId, String spellId) {
        if (!spellId.isEmpty()) {
            PENDING_PRESENTATIONS.put(entityId, spellId);
        }
    }

    public static boolean activatePending(UUID entityId, String spellId) {
        var pendingSpellId = PENDING_PRESENTATIONS.get(entityId);
        if (pendingSpellId == null || !pendingSpellId.equals(spellId)) {
            return false;
        }

        PENDING_PRESENTATIONS.remove(entityId);
        ACTIVE_PRESENTATIONS.put(entityId, spellId);
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
        var pendingSpellId = PENDING_PRESENTATIONS.get(entityId);
        return pendingSpellId != null && (spellId == null || pendingSpellId.equals(spellId));
    }

    public static boolean hasActive(UUID entityId, @Nullable String spellId) {
        var activeSpellId = ACTIVE_PRESENTATIONS.get(entityId);
        return activeSpellId != null && (spellId == null || activeSpellId.equals(spellId));
    }
}
