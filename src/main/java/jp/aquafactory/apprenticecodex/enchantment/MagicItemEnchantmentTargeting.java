package jp.aquafactory.apprenticecodex.enchantment;

import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBow;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbow;
import jp.aquafactory.apprenticecodex.item.smashcastscepter.SmashcastScepter;
import net.minecraft.world.item.Item;

final class MagicItemEnchantmentTargeting {
    private MagicItemEnchantmentTargeting() {
    }

    static boolean isSupportedSynthesisEnchantingItem(Item item) {
        return item instanceof FocusStaffbow || item instanceof ElementalBow;
    }

    static boolean isSupportedSmashcastScepterItem(Item item) {
        return item instanceof SmashcastScepter;
    }
}
