package jp.aquafactory.apprenticecodex.item.swingstaff;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.UUID;

public final class SwingcastStaffCastContext implements AutoCloseable {
    private static final ThreadLocal<Deque<Entry>> ACTIVE_CONTEXTS = ThreadLocal.withInitial(ArrayDeque::new);

    private final Entry entry;
    private boolean closed;

    private SwingcastStaffCastContext(Entry entry) {
        this.entry = entry;
    }

    public static SwingcastStaffCastContext open(UUID playerId, ItemStack stack, AbstractSpell spell) {
        var entry = new Entry(playerId, stack.copy(), spell.getSpellId());
        ACTIVE_CONTEXTS.get().push(entry);
        return new SwingcastStaffCastContext(entry);
    }

    public static Optional<ItemStack> getCastingStack(UUID playerId, AbstractSpell spell) {
        if (!matches(playerId, spell.getSpellId())) {
            return Optional.empty();
        }

        var current = ACTIVE_CONTEXTS.get().peek();
        return current == null ? Optional.empty() : Optional.of(current.stack());
    }

    static boolean matches(UUID playerId, ItemStack stack, AbstractSpell spell) {
        if (!matches(playerId, spell.getSpellId())) {
            return false;
        }

        var current = ACTIVE_CONTEXTS.get().peek();
        return current != null && current.stack().getItem() == stack.getItem();
    }

    public static boolean matches(UUID playerId, String spellId) {
        var contexts = ACTIVE_CONTEXTS.get();
        if (contexts.isEmpty()) {
            return false;
        }

        var current = contexts.peek();
        return current != null
                && current.playerId().equals(playerId)
                && current.spellId().equals(spellId);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;
        var contexts = ACTIVE_CONTEXTS.get();
        if (!contexts.isEmpty() && contexts.peek() == entry) {
            contexts.pop();
        } else {
            contexts.remove(entry);
        }
    }

    private record Entry(UUID playerId, ItemStack stack, String spellId) {
    }
}
