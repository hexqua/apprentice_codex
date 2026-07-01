package jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class MithrilFreecastStaffCastContext implements AutoCloseable {
    private static final Map<Key, Entry> PENDING_COOLDOWN_SOURCES = new HashMap<>();
    private static final Map<Key, Entry> RESOLVED_COOLDOWN_SOURCES = new HashMap<>();

    @Nullable
    private static Entry activeEntry;

    private final Entry entry;

    private MithrilFreecastStaffCastContext(@Nullable Entry entry) {
        this.entry = entry;
        activeEntry = entry;
    }

    public static MithrilFreecastStaffCastContext open(
            UUID playerId,
            ItemStack stack,
            AbstractSpell spell,
            CastSource selectedCastSource,
            ItemStack selectedStack
    ) {
        var entry = createEntry(playerId, stack, spell, selectedCastSource, selectedStack);
        return new MithrilFreecastStaffCastContext(entry);
    }

    public static void retainUntilCooldown(
            UUID playerId,
            ItemStack stack,
            AbstractSpell spell,
            CastSource selectedCastSource,
            ItemStack selectedStack
    ) {
        var entry = createEntry(playerId, stack, spell, selectedCastSource, selectedStack);
        PENDING_COOLDOWN_SOURCES.put(entry.key(), entry);
    }

    public static Optional<CooldownSource> consumeCooldownSource(UUID playerId, ItemStack stack, AbstractSpell spell) {
        var key = Key.of(playerId, stack, spell);
        var entry = activeEntry;
        if (entry != null && entry.key().equals(key)) {
            PENDING_COOLDOWN_SOURCES.remove(key);
            return Optional.of(entry.toCooldownSource());
        }

        entry = PENDING_COOLDOWN_SOURCES.remove(key);
        if (entry != null) {
            RESOLVED_COOLDOWN_SOURCES.put(key, entry);
            return Optional.of(entry.toCooldownSource());
        }

        return Optional.empty();
    }

    public static Optional<CooldownSource> resolveCooldownSource(UUID playerId, ItemStack stack, AbstractSpell spell) {
        var key = Key.of(playerId, stack, spell);
        var entry = activeEntry;
        if (entry != null && entry.key().equals(key)) {
            return Optional.of(entry.toCooldownSource());
        }

        entry = PENDING_COOLDOWN_SOURCES.get(key);
        if (entry != null) {
            return Optional.of(entry.toCooldownSource());
        }

        entry = RESOLVED_COOLDOWN_SOURCES.get(key);
        return entry == null ? Optional.empty() : Optional.of(entry.toCooldownSource());
    }

    public static void clearResolvedCooldownSource(UUID playerId, ItemStack stack, AbstractSpell spell) {
        RESOLVED_COOLDOWN_SOURCES.remove(Key.of(playerId, stack, spell));
    }

    public static void clearPendingCooldownSource(UUID playerId, ItemStack stack, AbstractSpell spell) {
        PENDING_COOLDOWN_SOURCES.remove(Key.of(playerId, stack, spell));
        clearResolvedCooldownSource(playerId, stack, spell);
    }

    @Override
    public void close() {
        if (Objects.equals(activeEntry, entry)) {
            activeEntry = null;
        }
    }

    public record CooldownSource(CastSource castSource, ItemStack stack) {
    }

    private static Entry createEntry(
            UUID playerId,
            ItemStack stack,
            AbstractSpell spell,
            CastSource selectedCastSource,
            ItemStack selectedStack
    ) {
        return new Entry(
                Key.of(playerId, stack, spell),
                selectedCastSource,
                selectedStack.copy()
        );
    }

    private record Entry(Key key, CastSource selectedCastSource, ItemStack selectedStack) {
        private CooldownSource toCooldownSource() {
            return new CooldownSource(selectedCastSource, selectedStack.copy());
        }
    }

    private record Key(UUID playerId, Item item, String spellId) {
        private static Key of(UUID playerId, ItemStack stack, AbstractSpell spell) {
            return new Key(playerId, stack.getItem(), spell.getSpellId());
        }
    }
}
