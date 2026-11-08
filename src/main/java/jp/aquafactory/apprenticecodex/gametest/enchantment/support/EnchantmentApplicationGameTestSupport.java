package jp.aquafactory.apprenticecodex.gametest.enchantment.support;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashSet;
import java.util.Set;

public final class EnchantmentApplicationGameTestSupport {
    private EnchantmentApplicationGameTestSupport() {}

    public static ItemStack createEnchantedBook(Enchantment enchantment) {
        var book = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantedBookItem.addEnchantment(book, new EnchantmentInstance(enchantment, 1));
        return book;
    }

    @SafeVarargs
    public static Set<ResourceLocation> registryIdSet(RegistryObject<Enchantment>... enchantments) {
        var ids = new LinkedHashSet<ResourceLocation>();
        for (var enchantment : enchantments) {
            var id = enchantment.getId();
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    static String describeEnchantmentDifference(
            Set<ResourceLocation> expectedEnchantments,
            Set<ResourceLocation> actualEnchantments
    ) {
        var missingEnchantments = new LinkedHashSet<>(expectedEnchantments);
        missingEnchantments.removeAll(actualEnchantments);

        var unexpectedEnchantments = new LinkedHashSet<>(actualEnchantments);
        unexpectedEnchantments.removeAll(expectedEnchantments);

        return "missing=" + missingEnchantments + ", unexpected=" + unexpectedEnchantments;
    }
}
