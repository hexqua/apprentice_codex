package jp.aquafactory.apprenticecodex.enchantment;

import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.world.item.ItemStack;

/** Wisdom の効果対象を、既存のポリシーと任意依存向けのタグから解決する。 */
public final class WisdomResolver {
    private WisdomResolver() {
    }

    public static boolean isActiveWhileHeld(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(TagRegistry.Items.WISDOM_HELD)
                || stack.getItem() instanceof WisdomPolicy policy && policy.isWisdomActiveWhileHeld());
    }

    public static boolean isActiveAsArmor(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(TagRegistry.Items.WISDOM_ARMOR)
                || stack.getItem() instanceof WisdomPolicy);
    }

    public static boolean isActiveAsCurio(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(TagRegistry.Items.WISDOM_CURIOS)
                || stack.getItem() instanceof WisdomPolicy);
    }
}
