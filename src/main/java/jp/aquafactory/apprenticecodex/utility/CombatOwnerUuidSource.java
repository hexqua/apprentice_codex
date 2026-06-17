package jp.aquafactory.apprenticecodex.utility;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface CombatOwnerUuidSource {
    @Nullable UUID getCombatOwnerUuid();
}
