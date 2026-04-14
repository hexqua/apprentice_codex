package jp.aquafactory.apprenticecodex.enchantment;

import jp.aquafactory.apprenticecodex.item.flask.AbstractPotionFlaskItem;
import net.minecraft.world.item.Item;

final class FlaskEnchantmentTargeting {
    private FlaskEnchantmentTargeting() {
    }

    static boolean isSupportedFlaskItem(Item item) {
        return item instanceof AbstractPotionFlaskItem;
    }
}
