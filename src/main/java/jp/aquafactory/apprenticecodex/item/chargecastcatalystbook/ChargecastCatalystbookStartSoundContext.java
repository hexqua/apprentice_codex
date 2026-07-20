package jp.aquafactory.apprenticecodex.item.chargecastcatalystbook;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class ChargecastCatalystbookStartSoundContext {
    private static final ThreadLocal<Set<UUID>> SUPPRESSED_ENTITY_IDS = ThreadLocal.withInitial(HashSet::new);

    private ChargecastCatalystbookStartSoundContext() {
    }

    public static void runSuppressed(UUID entityId, Runnable action) {
        callSuppressed(entityId, () -> {
            action.run();
            return null;
        });
    }

    public static <T> T callSuppressed(UUID entityId, Supplier<T> action) {
        var suppressedEntityIds = SUPPRESSED_ENTITY_IDS.get();
        var added = suppressedEntityIds.add(entityId);
        try {
            return action.get();
        } finally {
            if (added) {
                suppressedEntityIds.remove(entityId);
                if (suppressedEntityIds.isEmpty()) {
                    SUPPRESSED_ENTITY_IDS.remove();
                }
            }
        }
    }

    public static boolean shouldSuppress(AbstractSpell spell, @Nullable LivingEntity entity) {
        return entity != null
                && SUPPRESSED_ENTITY_IDS.get().contains(entity.getUUID())
                && ChargecastCatalystbookPresentationResolver.shouldDeferStartSound(spell);
    }
}
