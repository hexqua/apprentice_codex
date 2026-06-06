package jp.aquafactory.apprenticecodex.item.scrollcastergauntlet;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public final class ScrollcasterGauntletFreecastContext implements AutoCloseable {
    @Nullable
    private static Entry activeEntry;

    private final Entry entry;

    private ScrollcasterGauntletFreecastContext(@Nullable Entry entry) {
        this.entry = entry;
        activeEntry = entry;
    }

    public static ScrollcasterGauntletFreecastContext open(UUID playerId, ItemStack stack, AbstractSpell spell) {
        var entry = new Entry(playerId, stack.getItem(), spell.getSpellId());
        return new ScrollcasterGauntletFreecastContext(entry);
    }

    public static boolean matches(UUID playerId, ItemStack stack, AbstractSpell spell) {
        var entry = activeEntry;
        return entry != null
                && entry.playerId().equals(playerId)
                && entry.item() == stack.getItem()
                && entry.spellId().equals(spell.getSpellId());
    }

    @Override
    public void close() {
        if (Objects.equals(activeEntry, entry)) {
            activeEntry = null;
        }
    }

    private record Entry(UUID playerId, Item item, String spellId) {
    }
}
