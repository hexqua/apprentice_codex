package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class RecastCooldownPolicyContext {
    @Nullable
    private static Entry activeEntry;

    private RecastCooldownPolicyContext() {
    }

    public static void begin(ServerPlayer player, RecastInstance recastInstance) {
        activeEntry = new Entry(player.getUUID(), recastInstance.getSpellId());
    }

    public static void end(ServerPlayer player, RecastInstance recastInstance) {
        var entry = activeEntry;
        if (entry != null && entry.matches(player, recastInstance)) {
            activeEntry = null;
        }
    }

    public static boolean isCompletingRecast(ServerPlayer player, AbstractSpell spell) {
        var entry = activeEntry;
        return entry != null && entry.matches(player, spell);
    }

    private record Entry(java.util.UUID playerId, String spellId) {
        private boolean matches(ServerPlayer player, AbstractSpell spell) {
            return playerId.equals(player.getUUID()) && spellId.equals(spell.getSpellId());
        }

        private boolean matches(ServerPlayer player, RecastInstance recastInstance) {
            return playerId.equals(player.getUUID()) && spellId.equals(recastInstance.getSpellId());
        }
    }
}
