package jp.aquafactory.apprenticecodex.enchantment;

import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.item.armor.EnchantressRobeItem;
import jp.aquafactory.apprenticecodex.item.armor.StealthRuneArmorItem;
import net.minecraft.world.item.Item;

final class MagicItemEnchantmentTargeting {
    private MagicItemEnchantmentTargeting() {
    }

    static boolean isSupportedMagicItem(Item item) {
        return isSupportedOffhandMagicItem(item)
                || isSupportedSpellGunItem(item)
                || isSupportedMagicArmorItem(item);
    }

    static boolean isSupportedOffhandMagicItem(Item item) {
        return item instanceof AbstractOffhandMagicItem;
    }

    static boolean isSupportedSpellGunItem(Item item) {
        return item instanceof AbstractSpellGunItem;
    }

    static boolean isSupportedMagicArmorItem(Item item) {
        return item instanceof EnchantressRobeItem;
    }

    static boolean isSupportedOffhandOrArmorMagicItem(Item item) {
        return isSupportedOffhandMagicItem(item) || isSupportedMagicArmorItem(item);
    }

    static boolean isSupportedSpellContainerMagicItem(Item item) {
        return isSupportedOffhandMagicItem(item)
                || isSupportedSpellGunItem(item)
                || item instanceof AbstractRightClickMagicWeaponItem
                || isSupportedSpellContainerArmorItem(item);
    }

    static boolean isSupportedSpellContainerArmorItem(Item item) {
        return item instanceof EnchantressRobeItem robeItem && robeItem.hasImbueSlot();
    }

    static boolean isSupportedWisdomEnchantingItem(Item item) {
        return item instanceof AbstractSpellGunItem
                || item instanceof AbstractRightClickMagicWeaponItem
                || item instanceof EnchantressRobeItem
                || item instanceof StealthRuneArmorItem;
    }

    static boolean isSupportedHeldWisdomMagicItem(Item item) {
        return item instanceof AbstractSpellGunItem || item instanceof AbstractRightClickMagicWeaponItem;
    }

    static boolean isSupportedLootingMagicItem(Item item) {
        return item instanceof AbstractSpellGunItem;
    }
}
