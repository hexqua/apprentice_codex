package jp.aquafactory.apprenticecodex.item.elementalbow;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import jp.aquafactory.apprenticecodex.item.TriggeredSpellCastHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ElementalBowCasting {
    private static final ThreadLocal<Context> ACTIVE = new ThreadLocal<>();

    private ElementalBowCasting() {
    }

    public static boolean isActive(Player player, AbstractSpell spell) {
        var context = ACTIVE.get();
        return context != null && context.player() == player && context.spell() == spell;
    }

    public static double manaMultiplier(Player player, AbstractSpell spell) {
        return isActive(player, spell) ? ACTIVE.get().manaMultiplier() : 1.0D;
    }

    public static boolean cast(Player player, ItemStack stack, AbstractSpell spell, int spellLevel) {
        if (spell.getCastType() == CastType.CONTINUOUS) return false;
        var previous = ACTIVE.get();
        String slot = player.getUsedItemHand() == InteractionHand.OFF_HAND
                ? SpellSelectionManager.OFFHAND : SpellSelectionManager.MAINHAND;
        // 通常魔法の cooldown を消す代わりに、弓の同期発動の間だけ判定と登録を除外する。
        try (var ignored = ElementalBowSpellPowerContext.open(player, spell, stack)) {
            ACTIVE.set(new Context(player, spell, ElementalBowRunes.manaMultiplier(stack, player)));
            if (!spell.attemptInitiateCast(stack, spellLevel, player.level(), player, CastSource.SWORD, true, slot))
                return false;
            var magicData = MagicData.getPlayerMagicData(player);
            if (spell.getCastType() == CastType.INSTANT) {
                // Iron's は INSTANT も次の tick まで保留するため、弓の補正が有効な間に発動と終了を済ませる。
                // MagicManager の INSTANT 経路と同じく、詠唱 tick コールバックは挟まない。
                spell.castSpell(player.level(), spellLevel, (ServerPlayer) player, magicData.getCastSource(), true);
                spell.onServerCastComplete(player.level(), spellLevel, player, magicData, false);
            } else {
                TriggeredSpellCastHelper.applyLongCastDurationOverride(player, spellLevel, spell, magicData, slot, 0);
            }
            return true;
        } finally {
            if (previous == null) ACTIVE.remove();
            else ACTIVE.set(previous);
        }
    }

    private record Context(Player player, AbstractSpell spell, double manaMultiplier) {
    }
}
