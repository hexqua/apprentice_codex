package jp.aquafactory.apprenticecodex.item;

import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightSpellSupport;
import jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter.ProtectionSpellSupporter;
import net.minecraft.server.level.ServerPlayer;

public final class SpellManaCostDiscounts {
    private SpellManaCostDiscounts() {
    }

    public static int applyKnownDiscounts(int manaCost, ServerPlayer player, String spellId) {
        // SpellOnCastEvent の疑似発火には弾消費などの副作用があるため、既知の割引だけを事前判定に再現する。
        var discountedManaCost = manaCost;
        if (CraftsmansDelightSpellSupport.isManaCostDiscountTarget(spellId)) {
            discountedManaCost = CraftsmansDelight.applyManaCostDiscount(discountedManaCost, player);
        }
        if (ProtectionSpellSupporter.isManaCostDiscountTargetSpell(spellId)) {
            discountedManaCost = ProtectionSpellSupporter.applyManaCostDiscount(discountedManaCost, player);
        }
        return Math.max(0, discountedManaCost);
    }
}
