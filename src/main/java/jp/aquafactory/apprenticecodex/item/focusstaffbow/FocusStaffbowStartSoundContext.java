package jp.aquafactory.apprenticecodex.item.focusstaffbow;

import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class FocusStaffbowStartSoundContext {
    private static final ThreadLocal<Set<UUID>> SUPPRESSED_ENTITY_IDS = ThreadLocal.withInitial(HashSet::new);

    private FocusStaffbowStartSoundContext() {
    }

    public static void runSuppressed(UUID entityId, Runnable action) {
        var suppressedEntityIds = SUPPRESSED_ENTITY_IDS.get();
        var added = suppressedEntityIds.add(entityId);
        try {
            action.run();
        } finally {
            if (added) {
                suppressedEntityIds.remove(entityId);
                if (suppressedEntityIds.isEmpty()) {
                    SUPPRESSED_ENTITY_IDS.remove();
                }
            }
        }
    }

    public static boolean isSuppressed(@Nullable LivingEntity entity) {
        return entity != null && SUPPRESSED_ENTITY_IDS.get().contains(entity.getUUID());
    }
}
