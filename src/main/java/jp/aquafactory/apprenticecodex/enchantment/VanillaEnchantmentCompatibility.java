package jp.aquafactory.apprenticecodex.enchantment;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.function.Predicate;

/**
 * 防具の独自エンチャント拡張で、バニラ防具と同じ適用面を維持するための共通判定。
 */
public final class VanillaEnchantmentCompatibility {
    private VanillaEnchantmentCompatibility() {
    }

    public static boolean isVanilla(Holder<Enchantment> enchantment) {
        return enchantment.unwrapKey()
                .map(key -> key.location().getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE))
                .orElse(false);
    }

    public static boolean isNonVanillaAndSupported(
            Holder<Enchantment> enchantment,
            Predicate<Holder<Enchantment>> supportedEnchantment
    ) {
        return !isVanilla(enchantment) && supportedEnchantment.test(enchantment);
    }

    public static boolean bookContainsOnlyVanillaOrSupported(
            ItemStack book,
            Predicate<Holder<Enchantment>> supportedEnchantment
    ) {
        return EnchantmentHelper.getEnchantmentsForCrafting(book).keySet().stream()
                .allMatch(enchantment -> isVanilla(enchantment) || supportedEnchantment.test(enchantment));
    }
}
