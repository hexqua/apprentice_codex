package jp.aquafactory.apprenticecodex.utility;

import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.RegistryObject;

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

    private static boolean matches(Enchantment enchantment, RegistryObject<Enchantment> registryObject) {
        return registryObject.isPresent() && registryObject.get() == enchantment;
    }
}
