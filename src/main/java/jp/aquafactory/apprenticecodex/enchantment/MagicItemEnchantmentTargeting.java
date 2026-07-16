package jp.aquafactory.apprenticecodex.enchantment;

import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.spellgun.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBow;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbow;
import jp.aquafactory.apprenticecodex.item.manaforceblade.ManaForceBlade;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaff;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifle;
import jp.aquafactory.apprenticecodex.item.OffhandMagicCompatibleItem;
import jp.aquafactory.apprenticecodex.item.pastelstaff.PastelStaff;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.item.smashcastscepter.SmashcastScepter;
import jp.aquafactory.apprenticecodex.item.armor.ChromaticMagiaDressItem;
import jp.aquafactory.apprenticecodex.item.armor.ElementMaidenRobeItem;
import jp.aquafactory.apprenticecodex.item.armor.EnchantressRobeItem;
import jp.aquafactory.apprenticecodex.item.armor.StealthRuneArmorItem;
import jp.aquafactory.apprenticecodex.item.flask.AlchemistsFlask;
import jp.aquafactory.apprenticecodex.item.spellsideedge.AbstractSpellSideEdgeItem;
import jp.aquafactory.apprenticecodex.item.shield.BulwarkGreatshield;
import jp.aquafactory.apprenticecodex.item.shield.ParrycastBuckler;
import jp.aquafactory.apprenticecodex.item.shield.ReflectcastShield;
import net.minecraft.world.item.Item;

final class MagicItemEnchantmentTargeting {
    private MagicItemEnchantmentTargeting() {
    }

    static boolean isSupportedWisdomEnchantingItem(Item item) {
        return item instanceof AbstractSpellGunItem
                || item instanceof ElementalBow
                || item instanceof ManaForceBlade
                || item instanceof AbstractSpellSideEdgeItem
                || item instanceof PastelStaff
                || item instanceof MulticastEchoStaff
                || item instanceof MultipurposeStaffrifle
                || item instanceof ScrollcasterGauntlet
                || item instanceof AbstractRightClickMagicWeaponItem
                || item instanceof AlchemistsFlask
                || item instanceof EnchantressRobeItem
                || item instanceof StealthRuneArmorItem
                || item instanceof ChromaticMagiaDressItem
                || item instanceof ElementMaidenRobeItem
                || item instanceof OffhandMagicCompatibleItem
                || item instanceof BulwarkGreatshield
                || item instanceof ReflectcastShield
                || item instanceof ParrycastBuckler;
    }

    static boolean isSupportedHeldWisdomMagicItem(Item item) {
        return item instanceof AbstractSpellGunItem
                || item instanceof ElementalBow
                || item instanceof ManaForceBlade
                || item instanceof AbstractSpellSideEdgeItem
                || item instanceof PastelStaff
                || item instanceof MulticastEchoStaff
                || item instanceof MultipurposeStaffrifle
                || item instanceof ScrollcasterGauntlet
                || item instanceof AbstractRightClickMagicWeaponItem
                || item instanceof AlchemistsFlask
                || item instanceof BulwarkGreatshield
                || item instanceof ReflectcastShield
                || item instanceof ParrycastBuckler;
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
