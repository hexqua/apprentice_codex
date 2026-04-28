package jp.aquafactory.apprenticecodex.enchantment;

import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.item.ElementalBow;
import jp.aquafactory.apprenticecodex.item.FocusStaffbow;
import jp.aquafactory.apprenticecodex.item.ManaForceBlade;
import jp.aquafactory.apprenticecodex.item.OffhandMagicCompatibleItem;
import jp.aquafactory.apprenticecodex.item.armor.EnchantressRobeItem;
import jp.aquafactory.apprenticecodex.item.armor.StealthRuneArmorItem;
import jp.aquafactory.apprenticecodex.item.flask.AlchemistsFlask;
import net.minecraft.world.item.Item;

final class MagicItemEnchantmentTargeting {
    private MagicItemEnchantmentTargeting() {
    }

    static boolean isSupportedMagicItem(Item item) {
        return isSupportedOffhandMagicItem(item)
                || isSupportedSpellGunItem(item)
                || isSupportedMagicArmorItem(item);
    }

    static boolean isSupportedSurgeMagicItem(Item item) {
        return isSupportedMagicItem(item) || item instanceof ManaForceBlade;
    }

    static boolean isSupportedOffhandMagicItem(Item item) {
        return item instanceof AbstractOffhandMagicItem || item instanceof OffhandMagicCompatibleItem;
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
                || item instanceof ElementalBow
                || item instanceof ManaForceBlade
                || item instanceof AbstractRightClickMagicWeaponItem
                || item instanceof AlchemistsFlask
                || isSupportedSpellContainerArmorItem(item);
    }

    static boolean isSupportedSpellContainerArmorItem(Item item) {
        return item instanceof EnchantressRobeItem robeItem && robeItem.hasImbueSlot();
    }

    static boolean isSupportedWisdomEnchantingItem(Item item) {
        return item instanceof AbstractSpellGunItem
                || item instanceof ElementalBow
                || item instanceof ManaForceBlade
                || item instanceof AbstractRightClickMagicWeaponItem
                || item instanceof AlchemistsFlask
                || item instanceof EnchantressRobeItem
                || item instanceof StealthRuneArmorItem
                || item instanceof OffhandMagicCompatibleItem;
    }

    static boolean isSupportedHeldWisdomMagicItem(Item item) {
        return item instanceof AbstractSpellGunItem
                || item instanceof ElementalBow
                || item instanceof ManaForceBlade
                || item instanceof AbstractRightClickMagicWeaponItem
                || item instanceof AlchemistsFlask;
    }

    static boolean isSupportedLootingMagicItem(Item item) {
        return item instanceof AbstractSpellGunItem
                || item instanceof ElementalBow;
    }

    static boolean isSupportedSynthesisEnchantingItem(Item item) {
        return item instanceof FocusStaffbow;
    }
}
