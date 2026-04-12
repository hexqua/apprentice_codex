package jp.aquafactory.apprenticecodex.utility;

import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Objects;

public final class ApprenticeEnchantmentAvailability {
    private ApprenticeEnchantmentAvailability() {
    }

    public static boolean isFlaskExclusiveEnchantment(Enchantment enchantment) {
        var enchantmentId = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
        return Objects.equals(enchantmentId, Enchantments.GUZZLE.location())
                || Objects.equals(enchantmentId, Enchantments.LARGE_MUG.location())
                || Objects.equals(enchantmentId, Enchantments.RED_ENERGY.location())
                || Objects.equals(enchantmentId, Enchantments.GLOW_ENERGY.location());
    }
}
