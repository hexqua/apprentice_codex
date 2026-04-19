package jp.aquafactory.apprenticecodex.item.focusstaffbow;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class FocusStaffbowClientPresentationState {
    private static final Map<UUID, String> PENDING_PRESENTATIONS = new HashMap<>();

    private FocusStaffbowClientPresentationState() {
    }

    public static void markPending(UUID entityId, String spellId) {
        if (!spellId.isEmpty()) {
            PENDING_PRESENTATIONS.put(entityId, spellId);
        }
    }

    public static boolean consumePending(UUID entityId, String spellId) {
        var pendingSpellId = PENDING_PRESENTATIONS.get(entityId);
        if (pendingSpellId == null || !pendingSpellId.equals(spellId)) {
            return false;
        }

        PENDING_PRESENTATIONS.remove(entityId);
        return true;
    }

    public static void clear(UUID entityId) {
        PENDING_PRESENTATIONS.remove(entityId);
    }

    public static void clearAll() {
        PENDING_PRESENTATIONS.clear();
    }

    public static boolean hasPending(UUID entityId, @Nullable String spellId) {
        var pendingSpellId = PENDING_PRESENTATIONS.get(entityId);
        return pendingSpellId != null && (spellId == null || pendingSpellId.equals(spellId));
    }
}
