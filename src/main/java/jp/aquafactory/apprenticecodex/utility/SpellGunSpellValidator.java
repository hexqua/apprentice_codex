package jp.aquafactory.apprenticecodex.utility;

import io.redspace.ironsspellbooks.item.Scroll;
import jp.aquafactory.apprenticecodex.item.ArcaneAnvilImbueBlockItem;
import jp.aquafactory.apprenticecodex.item.ArcaneAnvilScrollImbueBlockItem;
import net.minecraft.world.item.ItemStack;

public final class SpellGunSpellValidator {
    private SpellGunSpellValidator() {
    }

    public static boolean isUnsupportedArcaneAnvilSpell(ItemStack baseItemStack, ItemStack modifierItemStack) {
        if (baseItemStack.getItem() instanceof ArcaneAnvilScrollImbueBlockItem) {
            return modifierItemStack.getItem() instanceof Scroll;
        }
        if (baseItemStack.getItem() instanceof ArcaneAnvilImbueBlockItem) {
            return modifierItemStack.getItem() instanceof Scroll;
        }

        // 通常装備の適合条件は注入を拒否せず、術式調整台の受付と装備の発動時に判定する。
        return false;
    }
}
