package jp.aquafactory.apprenticecodex.enchantment;

import jp.aquafactory.apprenticecodex.item.curios.manashieldcharm.ManaShieldCharm;
import net.minecraft.world.item.Item;

final class ManaShieldCharmEnchantmentTargeting {
    private ManaShieldCharmEnchantmentTargeting() {
    }

    static boolean isSupportedManaShieldCharm(Item item) {
        return item instanceof ManaShieldCharm;
    }
}
