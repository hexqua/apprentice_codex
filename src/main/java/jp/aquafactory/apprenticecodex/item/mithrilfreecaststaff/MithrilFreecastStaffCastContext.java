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
            CastSource selectedCastSource
    ) {
        var entry = createEntry(playerId, stack, spell, selectedCastSource);
        return new MithrilFreecastStaffCastContext(entry);
    }

    public static void retainUntilCooldown(
            UUID playerId,
            ItemStack stack,
            AbstractSpell spell,
            CastSource selectedCastSource
    ) {
        var entry = createEntry(playerId, stack, spell, selectedCastSource);
        if (activeEntry != null && activeEntry.key().equals(entry.key()) && activeEntry.cooldownConsumed) {
            return;
        }
        PENDING_COOLDOWN_SOURCES.put(entry.key(), entry);
    }

    public static Optional<CooldownSource> consumeCooldownSource(UUID playerId, ItemStack stack, AbstractSpell spell) {
        var key = Key.of(playerId, stack, spell);
        var entry = activeEntry;
        if (entry != null && entry.key().equals(key)) {
            entry.cooldownConsumed = true;
            PENDING_COOLDOWN_SOURCES.remove(key);
            return Optional.of(entry.toCooldownSource());
        }

        entry = PENDING_COOLDOWN_SOURCES.remove(key);
        if (entry != null) {
            RESOLVED_COOLDOWN_SOURCES.put(key, entry);
            return Optional.of(entry.toCooldownSource());
        }

        return consumeRetainedCooldownSource(playerId, spell);
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
        if (entry != null) {
            return Optional.of(entry.toCooldownSource());
        }

        return resolveRetainedCooldownSource(playerId, spell);
    }

    public static void clearResolvedCooldownSource(UUID playerId, ItemStack stack, AbstractSpell spell) {
        RESOLVED_COOLDOWN_SOURCES.remove(Key.of(playerId, stack, spell));
        clearResolvedCooldownSource(playerId, spell);
    }

    public static void clearResolvedCooldownSource(UUID playerId, AbstractSpell spell) {
        RESOLVED_COOLDOWN_SOURCES.keySet().removeIf(key -> key.matches(playerId, spell));
    }

    public static void clearPendingCooldownSource(UUID playerId, ItemStack stack, AbstractSpell spell) {
        PENDING_COOLDOWN_SOURCES.remove(Key.of(playerId, stack, spell));
        clearPendingCooldownSource(playerId, spell);
        clearResolvedCooldownSource(playerId, stack, spell);
    }

    public static void clearPendingCooldownSource(UUID playerId, AbstractSpell spell) {
        PENDING_COOLDOWN_SOURCES.keySet().removeIf(key -> key.matches(playerId, spell));
        clearResolvedCooldownSource(playerId, spell);
    }

    @Override
    public void close() {
        if (Objects.equals(activeEntry, entry)) {
            activeEntry = null;
        }
    }

    public record CooldownSource(CastSource castSource, Item item) {
    }

    private static Optional<CooldownSource> consumeRetainedCooldownSource(UUID playerId, AbstractSpell spell) {
        for (var iterator = PENDING_COOLDOWN_SOURCES.entrySet().iterator(); iterator.hasNext(); ) {
            var mapEntry = iterator.next();
            if (mapEntry.getKey().matches(playerId, spell)) {
                iterator.remove();
                RESOLVED_COOLDOWN_SOURCES.put(mapEntry.getKey(), mapEntry.getValue());
                return Optional.of(mapEntry.getValue().toCooldownSource());
            }
        }

        return Optional.empty();
    }

    private static Optional<CooldownSource> resolveRetainedCooldownSource(UUID playerId, AbstractSpell spell) {
        var pendingEntry = findRetainedEntry(PENDING_COOLDOWN_SOURCES, playerId, spell);
        return pendingEntry.map(Entry::toCooldownSource).or(() -> findRetainedEntry(RESOLVED_COOLDOWN_SOURCES, playerId, spell)
                .map(Entry::toCooldownSource));

    }

    private static Optional<Entry> findRetainedEntry(Map<Key, Entry> entries, UUID playerId, AbstractSpell spell) {
        for (var mapEntry : entries.entrySet()) {
            if (mapEntry.getKey().matches(playerId, spell)) {
                return Optional.of(mapEntry.getValue());
            }
        }

        return Optional.empty();
    }

    private static Entry createEntry(
            UUID playerId,
            ItemStack stack,
            AbstractSpell spell,
            CastSource selectedCastSource
    ) {
        return new Entry(
                Key.of(playerId, stack, spell),
                selectedCastSource
        );
    }

    private static final class Entry {
        private final Key key;
        private final CastSource selectedCastSource;
        private boolean cooldownConsumed;

        private Entry(Key key, CastSource selectedCastSource) {
            this.key = key;
            this.selectedCastSource = selectedCastSource;
        }

        private Key key() {
            return key;
        }

        private CooldownSource toCooldownSource() {
            return new CooldownSource(selectedCastSource, key.item());
        }
    }

    private record Key(UUID playerId, Item item, String spellId) {
        private static Key of(UUID playerId, ItemStack stack, AbstractSpell spell) {
            return new Key(playerId, stack.getItem(), spell.getSpellId());
        }

        private boolean matches(UUID playerId, AbstractSpell spell) {
            return this.playerId.equals(playerId) && spellId.equals(spell.getSpellId());
        }
    }
}
