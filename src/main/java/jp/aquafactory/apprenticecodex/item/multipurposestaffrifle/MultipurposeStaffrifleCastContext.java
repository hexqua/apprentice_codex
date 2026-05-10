package jp.aquafactory.apprenticecodex.item.multipurposestaffrifle;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class MultipurposeStaffrifleCastContext {
    private static final ThreadLocal<Context> ACTIVE_CONTEXT = new ThreadLocal<>();
    private static final ConcurrentMap<UUID, Context> PENDING_CONTEXTS = new ConcurrentHashMap<>();
    private static final long PENDING_EXPIRE_TICKS = 40L;

    private MultipurposeStaffrifleCastContext() {
    }

    public static AutoCloseable open(UUID playerId, ItemStack castingStack, AbstractSpell spell, boolean recast) {
        var previous = ACTIVE_CONTEXT.get();
        ACTIVE_CONTEXT.set(new Context(previous, playerId, castingStack.getItem(), spell.getSpellId(), recast, -1L));
        return () -> {
            var current = ACTIVE_CONTEXT.get();
            if (current != null && current.previous() == previous) {
                if (previous == null) {
                    ACTIVE_CONTEXT.remove();
                } else {
                    ACTIVE_CONTEXT.set(previous);
                }
            } else if (previous == null) {
                ACTIVE_CONTEXT.remove();
            } else {
                ACTIVE_CONTEXT.set(previous);
            }
        };
    }

    public static void rememberPending(UUID playerId, ItemStack castingStack, AbstractSpell spell, boolean recast, long gameTime) {
        PENDING_CONTEXTS.put(playerId, new Context(null, playerId, castingStack.getItem(), spell.getSpellId(), recast, gameTime));
    }

    public static boolean isActiveFor(UUID playerId, ItemStack castingStack, @Nullable AbstractSpell spell) {
        var context = getMatchingContext(playerId, castingStack, spell);
        return context != null;
    }

    public static boolean isActiveRecastFor(UUID playerId, ItemStack castingStack, @Nullable AbstractSpell spell) {
        var context = getMatchingContext(playerId, castingStack, spell);
        return context != null && context.recast();
    }

    public static void clearPendingIfMatches(UUID playerId, ItemStack castingStack, @Nullable AbstractSpell spell) {
        var context = PENDING_CONTEXTS.get(playerId);
        if (matches(context, playerId, castingStack, spell)) {
            PENDING_CONTEXTS.remove(playerId, context);
        }
    }

    public static void clearExpiredPending(UUID playerId, long gameTime) {
        var context = PENDING_CONTEXTS.get(playerId);
        if (context != null && gameTime - context.gameTime() > PENDING_EXPIRE_TICKS) {
            PENDING_CONTEXTS.remove(playerId, context);
        }
    }

    private static Context getMatchingContext(UUID playerId, ItemStack castingStack, @Nullable AbstractSpell spell) {
        var context = ACTIVE_CONTEXT.get();
        if (matches(context, playerId, castingStack, spell)) {
            return context;
        }

        context = PENDING_CONTEXTS.get(playerId);
        return matches(context, playerId, castingStack, spell) ? context : null;
    }

    private static boolean matches(@Nullable Context context, UUID playerId, ItemStack castingStack, @Nullable AbstractSpell spell) {
        return context != null
                && spell != null
                && context.playerId().equals(playerId)
                && context.item() == castingStack.getItem()
                && context.spellId().equals(spell.getSpellId());
    }

    public static boolean isActiveSpell(@Nullable AbstractSpell spell) {
        var context = ACTIVE_CONTEXT.get();
        return context != null && spell != null && context.spellId().equals(spell.getSpellId());
    }

    private record Context(
            @Nullable Context previous,
            UUID playerId,
            net.minecraft.world.item.Item item,
            String spellId,
            boolean recast,
            long gameTime
    ) {
        private Context {
            Objects.requireNonNull(playerId);
            Objects.requireNonNull(item);
            Objects.requireNonNull(spellId);
        }
    }
}
