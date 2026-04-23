package jp.aquafactory.apprenticecodex.utility;

import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry.EnchantmentRef;
import net.minecraft.world.item.enchantment.Enchantment;

public final class ApprenticeEnchantmentAvailability {
    private ApprenticeEnchantmentAvailability() {
    }

    public static boolean isFlaskExclusiveEnchantment(Enchantment enchantment) {
        return matches(enchantment, EnchantmentRegistry.GUZZLE)
                || matches(enchantment, EnchantmentRegistry.LARGE_MUG)
                || matches(enchantment, EnchantmentRegistry.RED_ENERGY)
                || matches(enchantment, EnchantmentRegistry.GLOW_ENERGY);
    }

    public static boolean isExcludedFromRandomBookLoot(Enchantment enchantment) {
        return isFlaskExclusiveEnchantment(enchantment)
                || matches(enchantment, EnchantmentRegistry.SYNTHESIS)
                || matches(enchantment, EnchantmentRegistry.SHELL)
                || matches(enchantment, EnchantmentRegistry.SYNCHRONIZATION)
                || matches(enchantment, EnchantmentRegistry.NEUTRALIZATION);
    }

    private static boolean matches(Enchantment enchantment, EnchantmentRef enchantmentRef) {
        return enchantment.builtInRegistryHolder().is(enchantmentRef.key());
    }
}
