package jp.aquafactory.apprenticecodex.utility;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface CombatOwnerUuidHolder extends CombatOwnerUuidSource {
    String COMBAT_OWNER_UUID_TAG = "CombatOwnerUuid";

    void setCombatOwnerUuid(@Nullable UUID combatOwnerUuid);

    default void saveCombatOwnerUuid(CompoundTag tag) {
        var combatOwnerUuid = getCombatOwnerUuid();
        if (combatOwnerUuid != null) {
            tag.putUUID(COMBAT_OWNER_UUID_TAG, combatOwnerUuid);
        }
    }

    default void loadCombatOwnerUuid(CompoundTag tag) {
        setCombatOwnerUuid(tag.hasUUID(COMBAT_OWNER_UUID_TAG) ? tag.getUUID(COMBAT_OWNER_UUID_TAG) : null);
    }
}
