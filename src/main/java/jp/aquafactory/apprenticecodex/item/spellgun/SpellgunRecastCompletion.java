package jp.aquafactory.apprenticecodex.item.spellgun;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record SpellgunRecastCompletion(UUID playerId, String spellId, @Nullable SpellgunRecastCooldown cooldown) {
    private static final ThreadLocal<SpellgunRecastCompletion> CURRENT = new ThreadLocal<>();

    public static void run(ServerPlayer player, RecastInstance recast, Runnable action) {
        var previous = CURRENT.get();
        CURRENT.set(new SpellgunRecastCompletion(player.getUUID(), recast.getSpellId(),
                ((SpellgunRecastCooldown.Holder) recast).apprenticecodex$getCooldown()));
        try {
            action.run();
        } finally {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }

    public static @Nullable SpellgunRecastCompletion find(ServerPlayer player, AbstractSpell spell) {
        var context = CURRENT.get();
        return context != null && context.playerId.equals(player.getUUID()) && context.spellId.equals(spell.getSpellId())
                ? context : null;
    }
}
