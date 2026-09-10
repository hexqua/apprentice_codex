package jp.aquafactory.apprenticecodex.item.elementalbow;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.item.TriggeredSpellCastHelper;
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

    public static boolean cast(Player player, ItemStack stack, AbstractSpell spell, int spellLevel) {
        var previous = ACTIVE.get();
        String slot = player.getUsedItemHand() == InteractionHand.OFF_HAND
                ? SpellSelectionManager.OFFHAND : SpellSelectionManager.MAINHAND;
        // 通常魔法の cooldown を消す代わりに、弓の同期発動の間だけ判定と登録を除外する。
        try {
            ACTIVE.set(new Context(player, spell));
            if (!spell.attemptInitiateCast(stack, spellLevel, player.level(), player, CastSource.SWORD, true, slot))
                return false;
            TriggeredSpellCastHelper.applyLongCastDurationOverride(player, spellLevel, spell,
                    MagicData.getPlayerMagicData(player), slot, 0);
            return true;
        } finally {
            if (previous == null) ACTIVE.remove();
            else ACTIVE.set(previous);
        }
    }

    private record Context(Player player, AbstractSpell spell) {
    }
}
