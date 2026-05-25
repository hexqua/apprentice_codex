package jp.aquafactory.apprenticecodex.item.curios.satellitefollowcastamulet;

import java.util.HashMap;
import java.util.Map;

public final class SatelliteFollowcastAmuletClientState {
    private static final Map<CrystalKey, Long> ACTIVE_CRYSTALS = new HashMap<>();

    private SatelliteFollowcastAmuletClientState() {
    }

    public static void setContinuousActive(
            int ownerEntityId,
            String slotIdentifier,
            int curiosSlotIndex,
            int spellSlotIndex,
            boolean active,
            long activeUntilGameTime
    ) {
        var key = new CrystalKey(ownerEntityId, slotIdentifier, curiosSlotIndex, spellSlotIndex);
        if (active) {
            ACTIVE_CRYSTALS.put(key, Math.max(0L, activeUntilGameTime));
        } else {
            ACTIVE_CRYSTALS.remove(key);
        }
    }

    public static boolean isContinuousActive(
            int ownerEntityId,
            String slotIdentifier,
            int curiosSlotIndex,
            int spellSlotIndex,
            long gameTime
    ) {
        var key = new CrystalKey(ownerEntityId, slotIdentifier, curiosSlotIndex, spellSlotIndex);
        var activeUntilGameTime = ACTIVE_CRYSTALS.get(key);
        if (activeUntilGameTime == null) {
            return false;
        }
        if (activeUntilGameTime > 0L && gameTime > activeUntilGameTime) {
            ACTIVE_CRYSTALS.remove(key);
            return false;
        }
        return true;
    }

    private record CrystalKey(int ownerEntityId, String slotIdentifier, int curiosSlotIndex, int spellSlotIndex) {
    }
}
