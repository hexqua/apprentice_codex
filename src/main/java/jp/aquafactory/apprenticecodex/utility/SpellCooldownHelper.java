package jp.aquafactory.apprenticecodex.utility;

import io.redspace.ironsspellbooks.api.magic.MagicHelper;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import net.minecraft.server.level.ServerPlayer;

public final class SpellCooldownHelper {
    private SpellCooldownHelper() {
    }

    public static void addCooldownRespectingCreativeConfig(
            ServerPlayer player,
            AbstractSpell spell,
            CastSource castSource
    ) {
        // Iron's 1.20.1 では creativeCooldowns 判定が AbstractSpell.castSpell 内にあるため、
        // MagicManager を直接使う独自詠唱経路では同じ判定をここで補う。
        if (player.isCreative() && !ServerConfigs.CREATIVE_COOLDOWN.get()) {
            return;
        }

        MagicHelper.MAGIC_MANAGER.addCooldown(player, spell, castSource);
    }
}
