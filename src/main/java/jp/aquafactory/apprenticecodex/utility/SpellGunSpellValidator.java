package jp.aquafactory.apprenticecodex.utility;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.item.Scroll;
import jp.aquafactory.apprenticecodex.item.AbstractSpellGunItem;
import net.minecraft.world.item.ItemStack;

public final class SpellGunSpellValidator {
    private SpellGunSpellValidator() {
    }

    public static boolean isUnsupportedArcaneAnvilSpell(ItemStack baseItemStack, ItemStack modifierItemStack) {
        if (!(baseItemStack.getItem() instanceof AbstractSpellGunItem spellGunItem)
                || !(modifierItemStack.getItem() instanceof Scroll)) {
            return false;
        }

        var scrollContainer = ISpellContainer.get(modifierItemStack);
        if (scrollContainer == null) {
            return false;
        }

        var spellData = scrollContainer.getSpellAtIndex(0);
        return spellData != SpellData.EMPTY && !spellGunItem.canImbueSpell(spellData);
    }
}
