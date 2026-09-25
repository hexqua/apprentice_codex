package jp.aquafactory.apprenticecodex.item.spellgun;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.UUID;

public record SpellgunRecastCompletion(UUID playerId, String spellId, @Nullable SpellgunRecastCooldown cooldown) {
    private static final ThreadLocal<ArrayDeque<SpellgunRecastCompletion>> CURRENT =
            ThreadLocal.withInitial(ArrayDeque::new);

    public static void run(ServerPlayer player, RecastInstance recast, Runnable action) {
        begin(player, recast);
        try {
            action.run();
        } finally {
            end();
        }
    }

    public static void begin(ServerPlayer player, RecastInstance recast) {
        CURRENT.get().push(new SpellgunRecastCompletion(player.getUUID(), recast.getSpellId(),
                ((SpellgunRecastCooldown.Holder) recast).apprenticecodex$getCooldown()));
    }

    public static void end() {
        var stack = CURRENT.get();
        if (!stack.isEmpty()) stack.pop();
        if (stack.isEmpty()) CURRENT.remove();
    }

    public static @Nullable SpellgunRecastCompletion find(ServerPlayer player, AbstractSpell spell) {
        var context = CURRENT.get().peek();
        return context != null && context.playerId.equals(player.getUUID()) && context.spellId.equals(spell.getSpellId())
                ? context : null;
    }
}
