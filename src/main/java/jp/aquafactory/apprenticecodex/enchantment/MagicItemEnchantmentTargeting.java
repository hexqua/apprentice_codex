package jp.aquafactory.apprenticecodex.enchantment;

import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.item.AbstractSpellGunItem;
import net.minecraft.world.item.Item;

final class MagicItemEnchantmentTargeting {
    private MagicItemEnchantmentTargeting() {
    }

    static boolean isSupportedMagicItem(Item item) {
        return item instanceof AbstractOffhandMagicItem || item instanceof AbstractSpellGunItem;
    }

    static boolean isSupportedOffhandMagicItem(Item item) {
        return item instanceof AbstractOffhandMagicItem;
    }

    static boolean isSupportedSpellGunItem(Item item) {
        return item instanceof AbstractSpellGunItem;
    }
}
