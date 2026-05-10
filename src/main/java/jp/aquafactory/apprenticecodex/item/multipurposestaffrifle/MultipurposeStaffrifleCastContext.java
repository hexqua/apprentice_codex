package jp.aquafactory.apprenticecodex.item.multipurposestaffrifle;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public final class MultipurposeStaffrifleCastContext {
    private static final ThreadLocal<Context> ACTIVE_CONTEXT = new ThreadLocal<>();

    private MultipurposeStaffrifleCastContext() {
    }

    public static AutoCloseable open(UUID playerId, ItemStack castingStack, AbstractSpell spell, boolean recast) {
        var previous = ACTIVE_CONTEXT.get();
        ACTIVE_CONTEXT.set(new Context(previous, playerId, castingStack.getItem(), spell.getSpellId(), recast));
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

    public static boolean isActiveFor(UUID playerId, ItemStack castingStack, @Nullable AbstractSpell spell) {
        var context = ACTIVE_CONTEXT.get();
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

    public static boolean isActiveRecastFor(UUID playerId, ItemStack castingStack, @Nullable AbstractSpell spell) {
        var context = ACTIVE_CONTEXT.get();
        return context != null
                && context.recast()
                && isActiveFor(playerId, castingStack, spell);
    }

    private record Context(
            @Nullable Context previous,
            UUID playerId,
            net.minecraft.world.item.Item item,
            String spellId,
            boolean recast
    ) {
        private Context {
            Objects.requireNonNull(playerId);
            Objects.requireNonNull(item);
            Objects.requireNonNull(spellId);
        }
    }
}
