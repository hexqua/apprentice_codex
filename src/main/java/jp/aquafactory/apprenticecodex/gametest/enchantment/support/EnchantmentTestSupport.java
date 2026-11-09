package jp.aquafactory.apprenticecodex.gametest.enchantment.support;

import jp.aquafactory.apprenticecodex.compat.malum.MalumHauntedCompat;
import jp.aquafactory.apprenticecodex.item.NonDamageableAnvilMergeItem;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static jp.aquafactory.apprenticecodex.gametest.enchantment.application.EnchantmentApplicationGameTestScenarios.swordEnchantments;
import static jp.aquafactory.apprenticecodex.gametest.integration.malum.EnchantmentApplicationGameTestSupport.MALUM_REPLENISHING;
import static jp.aquafactory.apprenticecodex.gametest.integration.malum.EnchantmentApplicationGameTestSupport.MALUM_SPIRIT_PLUNDER;

public final class EnchantmentTestSupport {
    private EnchantmentTestSupport() {}

    public static Set<ResourceLocation> expectedRandomBookLootEnchantments() {
        return registryIdSet(
                EnchantmentRegistry.ALACRITY,
                EnchantmentRegistry.REFLUX,
                EnchantmentRegistry.RESERVOIR,
                EnchantmentRegistry.SURGE,
                EnchantmentRegistry.ATTUNEMENT,
                EnchantmentRegistry.TENSE,
                EnchantmentRegistry.TRANSCENDENCE,
                EnchantmentRegistry.WISDOM,
                EnchantmentRegistry.PLUNDER
        );
    }

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

    public static ItemStack createEnchantedBook(EnchantmentInstance... enchantments) {
        var book = new ItemStack(Items.ENCHANTED_BOOK);
        for (var enchantment : enchantments) {
            EnchantedBookItem.addEnchantment(book, enchantment);
        }
        return book;
    }

    public static String describeEnchantmentDifference(
            Set<ResourceLocation> expectedEnchantments,
            Set<ResourceLocation> actualEnchantments
    ) {
        var missingEnchantments = new LinkedHashSet<>(expectedEnchantments);
        missingEnchantments.removeAll(actualEnchantments);

        var unexpectedEnchantments = new LinkedHashSet<>(actualEnchantments);
        unexpectedEnchantments.removeAll(expectedEnchantments);

        return "missing=" + missingEnchantments + ", unexpected=" + unexpectedEnchantments;
    }

    public static Set<ResourceLocation> expectedSpellGunEnchantments(ItemStack stack) {
        var expected = registryIdSet(EnchantmentRegistry.ALACRITY, EnchantmentRegistry.REFLUX,
                EnchantmentRegistry.RESERVOIR, EnchantmentRegistry.SURGE, EnchantmentRegistry.ATTUNEMENT,
                EnchantmentRegistry.TENSE, EnchantmentRegistry.WISDOM,
                EnchantmentRegistry.PLUNDER);
        addExpectedMalumSpiritPlunderIfPresent(stack, expected);
        return expected;
    }

    public static Set<ResourceLocation> expectedOffhandEnchantments() {
        return registryIdSet(EnchantmentRegistry.ALACRITY, EnchantmentRegistry.REFLUX,
                EnchantmentRegistry.RESERVOIR, EnchantmentRegistry.SURGE, EnchantmentRegistry.ATTUNEMENT,
                EnchantmentRegistry.TENSE);
    }

    public static Set<ResourceLocation> expectedEnchantedCircletEnchantments() {
        var expected = new LinkedHashSet<>(expectedOffhandEnchantments());
        expected.addAll(registryIdSet(EnchantmentRegistry.WISDOM));
        return expected;
    }

    public static Set<ResourceLocation> expectedChargedTwinBladeStaffEnchantments(ItemStack stack) {
        var sword = new ItemStack(Items.DIAMOND_SWORD);
        var trident = new ItemStack(Items.TRIDENT);
        var expected = collectAllowedEnchantments(enchantment ->
                enchantment.canApplyAtEnchantingTable(sword) && !isDurabilityTargetEnchantment(enchantment));
        expected.addAll(collectAllowedEnchantments(enchantment ->
                enchantment.canApplyAtEnchantingTable(trident) && !isDurabilityTargetEnchantment(enchantment)));
        expected.addAll(registryIdSet(EnchantmentRegistry.WISDOM));
        addExpectedMalumHauntedIfPresent(stack, expected);
        addExpectedMalumReplenishingIfPresent(expected);
        return expected;
    }

    public static Set<ResourceLocation> expectedSpellSideEdgeEnchantments(ItemStack stack) {
        var expected = swordEnchantments(true);
        expected.addAll(registryIdSet(EnchantmentRegistry.WISDOM,
                EnchantmentRegistry.ALACRITY, EnchantmentRegistry.REFLUX,
                EnchantmentRegistry.RESERVOIR, EnchantmentRegistry.TENSE));
        addExpectedMalumSpiritPlunderIfPresent(stack, expected);
        return expected;
    }

    public static ItemStack createArmorProbeStack(ItemStack stack) {
        if (!(stack.getItem() instanceof ArmorItem armorItem)) {
            throw new IllegalArgumentException("Expected armor item for enchantment probe: " + stack);
        }
        return switch (armorItem.getType()) {
            case HELMET -> new ItemStack(Items.LEATHER_HELMET);
            case CHESTPLATE -> new ItemStack(Items.LEATHER_CHESTPLATE);
            case LEGGINGS -> new ItemStack(Items.LEATHER_LEGGINGS);
            case BOOTS -> new ItemStack(Items.LEATHER_BOOTS);
        };
    }

    public static final String MALUM_MOD_ID = "malum";

    public static final ResourceLocation MALUM_ANIMATED = MalumHauntedCompat.animatedEnchantmentId();

    public static final ResourceLocation MALUM_HAUNTED = MalumHauntedCompat.hauntedEnchantmentId();

    static final TagKey<Item> MALUM_SOUL_HUNTER_WEAPON = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "soul_hunter_weapon")
    );

    public static void assertCategoryEnchantments(
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

    public static void assertArmorCategoryEnchantments(
            GameTestHelper helper,
            String categoryName,
            Predicate<Item> itemPredicate,
            Function<ItemStack, Set<ResourceLocation>> expectedEnchantingTableEnchantmentsResolver,
            Function<ItemStack, Set<ResourceLocation>> expectedAnvilEnchantmentsResolver
    ) {
        var stacks = ItemRegistry.ITEMS.getEntries().stream()
                .map(RegistryObject::get)
                .filter(itemPredicate)
                .sorted(Comparator.comparing(item -> String.valueOf(ForgeRegistries.ITEMS.getKey(item))))
                .map(ItemStack::new)
                .toList();
        helper.assertFalse(stacks.isEmpty(), "No items matched enchantment test category: " + categoryName);

        var expectedBookEnchantments = getRegisteredEnchantmentIds();
        for (var stack : stacks) {
            assertExactEnchantmentSurfaces(
                    helper,
                    stack,
                    expectedEnchantingTableEnchantmentsResolver.apply(stack),
                    expectedBookEnchantments,
                    expectedAnvilEnchantmentsResolver.apply(stack),
                    categoryName + " " + ForgeRegistries.ITEMS.getKey(stack.getItem())
            );
        }
    }

    public static void assertExactEnchantmentSurfaces(
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

    public static void assertExactEnchantmentSurfaces(
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
                        + EnchantmentApplicationGameTestSupport.describeEnchantmentDifference(expectedEnchantingTableEnchantments, actualEnchantingTableEnchantments));

        var actualBookEnchantments = collectAllowedEnchantments(
                enchantment -> item.isBookEnchantable(stack, EnchantmentApplicationGameTestSupport.createEnchantedBook(enchantment))
        );
        helper.assertTrue(actualBookEnchantments.equals(expectedBookEnchantments),
                itemName + " book enchantments changed: "
                        + EnchantmentApplicationGameTestSupport.describeEnchantmentDifference(expectedBookEnchantments, actualBookEnchantments));

        var actualAnvilEnchantments = collectAllowedEnchantments(enchantment ->
                item.isBookEnchantable(stack, EnchantmentApplicationGameTestSupport.createEnchantedBook(enchantment))
                        && enchantment.canEnchant(stack));
        helper.assertTrue(actualAnvilEnchantments.equals(expectedAnvilEnchantments),
                itemName + " anvil enchantments changed: "
                        + EnchantmentApplicationGameTestSupport.describeEnchantmentDifference(expectedAnvilEnchantments, actualAnvilEnchantments));

        if (!(item instanceof NonDamageableAnvilMergeItem mergeItem)) {
            return;
        }

        var actualSameItemAnvilEnchantments = collectAllowedEnchantments(
                enchantment -> mergeItem.isAnvilMergeEnchantmentAllowed(stack, enchantment)
        );
        helper.assertTrue(actualSameItemAnvilEnchantments.equals(expectedAnvilEnchantments),
                itemName + " same-item anvil enchantments changed: "
                        + EnchantmentApplicationGameTestSupport.describeEnchantmentDifference(expectedAnvilEnchantments, actualSameItemAnvilEnchantments));
    }

    public static Set<ResourceLocation> collectAllowedEnchantments(Predicate<Enchantment> predicate) {
        var allowedEnchantments = new LinkedHashSet<ResourceLocation>();
        for (var enchantment : getRegisteredEnchantments()) {
            var enchantmentId = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
            if (enchantmentId != null && predicate.test(enchantment)) {
                allowedEnchantments.add(enchantmentId);
            }
        }
        return allowedEnchantments;
    }

    public static List<Enchantment> getRegisteredEnchantments() {
        return ForgeRegistries.ENCHANTMENTS.getValues().stream()
                .sorted(Comparator.comparing(enchantment -> String.valueOf(ForgeRegistries.ENCHANTMENTS.getKey(enchantment))))
                .toList();
    }

    static Set<ResourceLocation> getRegisteredEnchantmentIds() {
        return getRegisteredEnchantments().stream()
                .map(ForgeRegistries.ENCHANTMENTS::getKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static boolean isDurabilityTargetEnchantment(Enchantment enchantment) {
        return enchantment.canApplyAtEnchantingTable(new ItemStack(Items.ELYTRA));
    }

    public static void addExpectedMalumSpiritPlunderIfPresent(
            ItemStack stack,
            Set<ResourceLocation> expectedEnchantments
    ) {
        if (ModList.get().isLoaded(MALUM_MOD_ID)
                && stack.is(MALUM_SOUL_HUNTER_WEAPON)) {
            expectedEnchantments.add(MALUM_SPIRIT_PLUNDER);
        }
    }

    public static void addExpectedMalumHauntedIfPresent(
            ItemStack stack,
            Set<ResourceLocation> expectedEnchantments
    ) {
        if (ModList.get().isLoaded(MALUM_MOD_ID)
                && MalumHauntedCompat.isSupportedHauntedMainhandItem(stack)) {
            expectedEnchantments.add(MALUM_HAUNTED);
        }
    }

    public static void addExpectedMalumReplenishingIfPresent(Set<ResourceLocation> expectedEnchantments) {
        if (ModList.get().isLoaded(MALUM_MOD_ID)) {
            expectedEnchantments.add(MALUM_REPLENISHING);
        }
    }
}
