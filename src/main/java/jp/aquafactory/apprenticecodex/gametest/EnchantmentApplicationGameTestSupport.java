package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.compat.malum.MalumHauntedCompat;
import jp.aquafactory.apprenticecodex.item.NonDamageableAnvilMergeItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

final class EnchantmentApplicationGameTestSupport {
    static final String MALUM_MOD_ID = "malum";
    static final ResourceLocation MALUM_ANIMATED = MalumHauntedCompat.animatedEnchantmentId();
    static final ResourceLocation MALUM_HAUNTED = MalumHauntedCompat.hauntedEnchantmentId();
    static final ResourceLocation MALUM_SPIRIT_PLUNDER =
            ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "spirit_plunder");
    static final ResourceLocation MALUM_REPLENISHING =
            ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "replenishing");
    private static final TagKey<Item> MALUM_SOUL_HUNTER_WEAPON = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "soul_hunter_weapon")
    );

    private EnchantmentApplicationGameTestSupport() {
    }

    static void assertCategoryEnchantments(
            GameTestHelper helper,
            String categoryName,
            Predicate<Item> itemPredicate,
            Function<ItemStack, Set<ResourceLocation>> expectedEnchantmentsResolver
    ) {
        var stacks = ItemRegistry.ITEMS.getEntries().stream()
                .map(RegistryObject::get)
                .filter(itemPredicate)
                .sorted(Comparator.comparing(item -> String.valueOf(ForgeRegistries.ITEMS.getKey(item))))
                .map(ItemStack::new)
                .toList();
        helper.assertFalse(stacks.isEmpty(), "No items matched enchantment test category: " + categoryName);

        for (var stack : stacks) {
            assertExactEnchantmentSurfaces(
                    helper,
                    stack,
                    expectedEnchantmentsResolver.apply(stack),
                    categoryName + " " + ForgeRegistries.ITEMS.getKey(stack.getItem())
            );
        }
    }

    static void assertExactEnchantmentSurfaces(
            GameTestHelper helper,
            ItemStack stack,
            Set<ResourceLocation> expectedEnchantments,
            String itemName
    ) {
        assertExactEnchantmentSurfaces(
                helper,
                stack,
                expectedEnchantments,
                expectedEnchantments,
                expectedEnchantments,
                itemName
        );
    }

    static void assertExactEnchantmentSurfaces(
            GameTestHelper helper,
            ItemStack stack,
            Set<ResourceLocation> expectedEnchantingTableEnchantments,
            Set<ResourceLocation> expectedBookEnchantments,
            Set<ResourceLocation> expectedAnvilEnchantments,
            String itemName
    ) {
        var item = stack.getItem();
        var actualEnchantingTableEnchantments = collectAllowedEnchantments(
                enchantment -> item.canApplyAtEnchantingTable(stack, enchantment)
        );
        helper.assertTrue(actualEnchantingTableEnchantments.equals(expectedEnchantingTableEnchantments),
                itemName + " enchanting-table enchantments changed: "
                        + describeEnchantmentDifference(expectedEnchantingTableEnchantments, actualEnchantingTableEnchantments));

        var actualBookEnchantments = collectAllowedEnchantments(
                enchantment -> item.isBookEnchantable(stack, createEnchantedBook(enchantment))
        );
        helper.assertTrue(actualBookEnchantments.equals(expectedBookEnchantments),
                itemName + " book enchantments changed: "
                        + describeEnchantmentDifference(expectedBookEnchantments, actualBookEnchantments));

        if (item instanceof NonDamageableAnvilMergeItem mergeItem) {
            var actualAnvilEnchantments = collectAllowedEnchantments(
                    enchantment -> mergeItem.isAnvilMergeEnchantmentAllowed(stack, enchantment)
            );
            helper.assertTrue(actualAnvilEnchantments.equals(expectedAnvilEnchantments),
                    itemName + " anvil enchantments changed: "
                            + describeEnchantmentDifference(expectedAnvilEnchantments, actualAnvilEnchantments));
        }
    }

    static Set<ResourceLocation> collectAllowedEnchantments(Predicate<Enchantment> predicate) {
        var allowedEnchantments = new LinkedHashSet<ResourceLocation>();
        for (var enchantment : getRegisteredEnchantments()) {
            var enchantmentId = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
            if (enchantmentId != null && predicate.test(enchantment)) {
                allowedEnchantments.add(enchantmentId);
            }
        }
        return allowedEnchantments;
    }

    static ItemStack createEnchantedBook(Enchantment enchantment) {
        var book = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantedBookItem.addEnchantment(book, new EnchantmentInstance(enchantment, 1));
        return book;
    }

    @SafeVarargs
    static Set<ResourceLocation> registryIdSet(RegistryObject<Enchantment>... enchantments) {
        var ids = new LinkedHashSet<ResourceLocation>();
        for (var enchantment : enchantments) {
            var id = enchantment.getId();
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    static List<Enchantment> getRegisteredEnchantments() {
        return ForgeRegistries.ENCHANTMENTS.getValues().stream()
                .sorted(Comparator.comparing(enchantment -> String.valueOf(ForgeRegistries.ENCHANTMENTS.getKey(enchantment))))
                .toList();
    }

    static boolean isDurabilityTargetEnchantment(Enchantment enchantment) {
        return enchantment.canApplyAtEnchantingTable(new ItemStack(Items.ELYTRA));
    }

    static void addExpectedMalumSpiritPlunderIfPresent(
            ItemStack stack,
            Set<ResourceLocation> expectedEnchantments
    ) {
        if (ModList.get().isLoaded(MALUM_MOD_ID)
                && stack.is(MALUM_SOUL_HUNTER_WEAPON)) {
            expectedEnchantments.add(MALUM_SPIRIT_PLUNDER);
        }
    }

    static void addExpectedMalumHauntedIfPresent(
            ItemStack stack,
            Set<ResourceLocation> expectedEnchantments
    ) {
        if (ModList.get().isLoaded(MALUM_MOD_ID)
                && MalumHauntedCompat.isSupportedHauntedMainhandItem(stack)) {
            expectedEnchantments.add(MALUM_HAUNTED);
        }
    }

    static void addExpectedMalumReplenishingIfPresent(Set<ResourceLocation> expectedEnchantments) {
        if (ModList.get().isLoaded(MALUM_MOD_ID)) {
            expectedEnchantments.add(MALUM_REPLENISHING);
        }
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
