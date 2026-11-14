package jp.aquafactory.apprenticecodex.enchantment;

import net.minecraft.world.item.ItemStack;

/** Wisdom の効果対象を、既存のポリシーと任意依存向けのタグから解決する。 */
public final class WisdomResolver {
    private WisdomResolver() {
    }

    public static boolean isActiveWhileHeld(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(Enchantments.WISDOM_HELD)
                || stack.getItem() instanceof WisdomPolicy policy && policy.isWisdomActiveWhileHeld());
    }

    public static boolean isActiveAsArmor(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(Enchantments.WISDOM_ARMOR)
                || stack.getItem() instanceof WisdomPolicy);
    }

    public static boolean isActiveAsCurio(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(Enchantments.WISDOM_CURIOS)
                || stack.getItem() instanceof WisdomPolicy);
    }
}
