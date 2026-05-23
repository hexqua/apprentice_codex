package jp.aquafactory.apprenticecodex.enchantment;

import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.item.ElementalBow;
import jp.aquafactory.apprenticecodex.item.FocusStaffbow;
import jp.aquafactory.apprenticecodex.item.ManaForceBlade;
import jp.aquafactory.apprenticecodex.item.MithrilFreecastStaff;
import jp.aquafactory.apprenticecodex.item.MulticastEchoStaff;
import jp.aquafactory.apprenticecodex.item.MultipurposeStaffrifle;
import jp.aquafactory.apprenticecodex.item.OffhandMagicCompatibleItem;
import jp.aquafactory.apprenticecodex.item.PastelStaff;
import jp.aquafactory.apprenticecodex.item.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.item.SmashcastScepter;
import jp.aquafactory.apprenticecodex.item.armor.ChromaticMagiaDressItem;
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
                || isSupportedMagicArmorItem(item)
                || item instanceof ScrollcasterGauntlet;
    }

    static boolean isSupportedSurgeMagicItem(Item item) {
        return isSupportedMagicItem(item) || item instanceof ManaForceBlade || item instanceof MultipurposeStaffrifle;
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
        return isSupportedOffhandMagicItem(item) || isSupportedMagicArmorItem(item) || item instanceof ScrollcasterGauntlet;
    }

    static boolean isSupportedSpellContainerMagicItem(Item item) {
        return isSupportedOffhandMagicItem(item)
                || isSupportedSpellGunItem(item)
                || item instanceof ElementalBow
                || item instanceof ManaForceBlade
                || item instanceof ScrollcasterGauntlet
                || (item instanceof AbstractRightClickMagicWeaponItem && !(item instanceof MithrilFreecastStaff))
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
                || item instanceof PastelStaff
                || item instanceof MulticastEchoStaff
                || item instanceof MultipurposeStaffrifle
                || item instanceof ScrollcasterGauntlet
                || item instanceof AbstractRightClickMagicWeaponItem
                || item instanceof AlchemistsFlask
                || item instanceof EnchantressRobeItem
                || item instanceof StealthRuneArmorItem
                || item instanceof ChromaticMagiaDressItem
                || item instanceof OffhandMagicCompatibleItem;
    }

    static boolean isSupportedHeldWisdomMagicItem(Item item) {
        return item instanceof AbstractSpellGunItem
                || item instanceof ElementalBow
                || item instanceof ManaForceBlade
                || item instanceof PastelStaff
                || item instanceof MulticastEchoStaff
                || item instanceof MultipurposeStaffrifle
                || item instanceof ScrollcasterGauntlet
                || item instanceof AbstractRightClickMagicWeaponItem
                || item instanceof AlchemistsFlask;
    }

    static boolean isSupportedLootingMagicItem(Item item) {
        return item instanceof AbstractSpellGunItem
                || item instanceof ElementalBow
                || item instanceof MultipurposeStaffrifle
                || item instanceof SmashcastScepter;
    }

    static boolean isSupportedSynthesisEnchantingItem(Item item) {
        return item instanceof FocusStaffbow || item instanceof ElementalBow;
    }

    static boolean isSupportedSmashcastScepterItem(Item item) {
        return item instanceof SmashcastScepter;
    }
}
