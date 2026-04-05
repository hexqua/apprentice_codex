package jp.aquafactory.apprenticecodex.item.swingstaff;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

final class SwingcastStaffCastContext implements AutoCloseable {
    private static final ThreadLocal<Deque<Entry>> ACTIVE_CONTEXTS = ThreadLocal.withInitial(ArrayDeque::new);

    private final Entry entry;
    private boolean closed;

    private SwingcastStaffCastContext(Entry entry) {
        this.entry = entry;
    }

    static SwingcastStaffCastContext open(UUID playerId, ItemStack stack, AbstractSpell spell) {
        var entry = new Entry(playerId, stack.getItem(), spell.getSpellId());
        ACTIVE_CONTEXTS.get().push(entry);
        return new SwingcastStaffCastContext(entry);
    }

    static boolean matches(UUID playerId, ItemStack stack, AbstractSpell spell) {
        var contexts = ACTIVE_CONTEXTS.get();
        if (contexts.isEmpty()) {
            return false;
        }

        var current = contexts.peek();
        return current != null
                && current.playerId().equals(playerId)
                && current.item() == stack.getItem()
                && current.spellId().equals(spell.getSpellId());
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

    private record Entry(UUID playerId, net.minecraft.world.item.Item item, String spellId) {
    }
}
