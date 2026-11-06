package jp.aquafactory.apprenticecodex.gametest.equipment.enchantment;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentPolicy;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.item.NonDamageableAnvilMergeItem;
import jp.aquafactory.apprenticecodex.item.armor.ElementMaidenRobeItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import static jp.aquafactory.apprenticecodex.gametest.integration.malum.MalumTestSupport.MALUM_ANIMATED;
import static jp.aquafactory.apprenticecodex.gametest.integration.malum.MalumTestSupport.MALUM_HAUNTED;
import static jp.aquafactory.apprenticecodex.gametest.integration.malum.MalumTestSupport.MALUM_MAGIC_CAPABLE_WEAPON;
import static jp.aquafactory.apprenticecodex.gametest.integration.malum.MalumTestSupport.MALUM_MOD_ID;
import static jp.aquafactory.apprenticecodex.gametest.integration.malum.MalumTestSupport.MALUM_REPLENISHING;
import static jp.aquafactory.apprenticecodex.gametest.integration.malum.MalumTestSupport.MALUM_SOUL_SHATTER_CAPABLE_WEAPON;
import static jp.aquafactory.apprenticecodex.gametest.integration.malum.MalumTestSupport.MALUM_SPIRIT_PLUNDER;

public final class EnchantmentTestSupport {
    private EnchantmentTestSupport() {}

    public static void assertCategoryEnchantments(
            GameTestHelper helper,
            String categoryName,
            Predicate<Item> itemPredicate,
            Set<ResourceLocation> expectedEnchantments
    ) {
        assertCategoryEnchantments(helper, categoryName, itemPredicate, stack -> expectedEnchantments);
    }

    public static void assertCategoryEnchantments(
            GameTestHelper helper,
            String categoryName,
            Predicate<Item> itemPredicate,
            Function<ItemStack, Set<ResourceLocation>> expectedEnchantmentsResolver
    ) {
        var stacks = getRegisteredItemStacks(itemPredicate);
        helper.assertFalse(stacks.isEmpty(), "No items matched enchantment test category: " + categoryName);

        for (var stack : stacks) {
            assertExactEnchantmentSurfaces(
                    helper,
                    stack,
                    expectedEnchantmentsResolver.apply(stack),
                    categoryName + " " + BuiltInRegistries.ITEM.getKey(stack.getItem())
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
                    expectedEnchantments,
                    expectedEnchantments,
                    itemName
        );
    }

    public static void assertExactEnchantmentSurfaces(
            GameTestHelper helper,
            ItemStack stack,
            Set<ResourceLocation> expectedPrimaryEnchantments,
            Set<ResourceLocation> expectedSupportedEnchantments,
            Set<ResourceLocation> expectedDefinitionEnchantments,
            Set<ResourceLocation> expectedBookEnchantments,
            Set<ResourceLocation> expectedAnvilEnchantments,
            String itemName
    ) {
        var item = stack.getItem();
        var registryAccess = helper.getLevel().registryAccess();

        var actualPrimaryEnchantments = collectAllowedEnchantments(
                registryAccess,
                enchantment -> item.isPrimaryItemFor(stack, enchantment)
        );
        helper.assertTrue(actualPrimaryEnchantments.equals(expectedPrimaryEnchantments),
                itemName + " primary enchantments changed: "
                        + describeEnchantmentDifference(expectedPrimaryEnchantments, actualPrimaryEnchantments));

        var actualSupportedEnchantments = collectAllowedEnchantments(
                registryAccess,
                enchantment -> item.supportsEnchantment(stack, enchantment)
        );
        helper.assertTrue(actualSupportedEnchantments.equals(expectedSupportedEnchantments),
                itemName + " supported enchantments changed: "
                        + describeEnchantmentDifference(expectedSupportedEnchantments, actualSupportedEnchantments));

        var actualDefinitionEnchantments = collectAllowedEnchantments(
                registryAccess,
                enchantment -> enchantment.value().canEnchant(stack)
        );
        helper.assertTrue(actualDefinitionEnchantments.equals(expectedDefinitionEnchantments),
                itemName + " enchantment definition support changed: "
                        + describeEnchantmentDifference(expectedDefinitionEnchantments, actualDefinitionEnchantments));

        var actualBookEnchantments = collectAllowedEnchantments(
                registryAccess,
                enchantment -> item.isBookEnchantable(stack, createEnchantedBook(enchantment))
        );
        helper.assertTrue(actualBookEnchantments.equals(expectedBookEnchantments),
                itemName + " book enchantments changed: "
                        + describeEnchantmentDifference(expectedBookEnchantments, actualBookEnchantments));

        if (item instanceof NonDamageableAnvilMergeItem mergeItem) {
            var actualAnvilEnchantments = collectAllowedEnchantments(
                    registryAccess,
                    enchantment -> mergeItem.isAnvilMergeEnchantmentAllowed(stack, enchantment)
            );
            helper.assertTrue(actualAnvilEnchantments.equals(expectedAnvilEnchantments),
                    itemName + " anvil enchantments changed: "
                            + describeEnchantmentDifference(expectedAnvilEnchantments, actualAnvilEnchantments));
        }
    }

    static void assertReferenceItemEnchantmentsWithRequiredExtras(
            GameTestHelper helper,
            ItemStack stack,
            ItemStack referenceStack,
            Set<ResourceLocation> requiredExtraEnchantments,
            String itemName
    ) {
        var registryAccess = helper.getLevel().registryAccess();
        var expectedPrimaryEnchantments = expectedReferencePrimaryEnchantments(registryAccess, referenceStack, requiredExtraEnchantments);
        var expectedSupportedEnchantments = expectedReferenceSupportedEnchantments(registryAccess, referenceStack, requiredExtraEnchantments);
        var expectedDefinitionEnchantments = expectedReferenceDefinitionEnchantments(registryAccess, referenceStack, requiredExtraEnchantments);
        var expectedBookEnchantments = expectedReferenceBookEnchantments(registryAccess, referenceStack, requiredExtraEnchantments);

        assertExactEnchantmentSurfaces(
                helper,
                stack,
                expectedPrimaryEnchantments,
                expectedSupportedEnchantments,
                expectedDefinitionEnchantments,
                expectedBookEnchantments,
                expectedSupportedEnchantments,
                itemName
        );

        var enchantmentLookup = registryAccess.lookupOrThrow(Registries.ENCHANTMENT);
        for (var enchantmentId : requiredExtraEnchantments) {
            assertSingleEnchantmentSurfaces(
                    helper,
                    stack,
                    enchantmentLookup.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, enchantmentId)),
                    true,
                    true,
                    true,
                    true,
                    stack.getItem() instanceof NonDamageableAnvilMergeItem ? Boolean.TRUE : null,
                    itemName + " required extra enchantment"
            );
        }
    }

    public static void assertRequiredExtraEnchantments(
            GameTestHelper helper,
            ItemStack stack,
            Set<ResourceLocation> requiredEnchantments,
            @Nullable Boolean expectedAnvil,
            String message
    ) {
        assertExtraEnchantments(helper, stack, requiredEnchantments, true, true, true, true, expectedAnvil, message);
    }

    public static void assertRejectedExtraEnchantments(
            GameTestHelper helper,
            ItemStack stack,
            Set<ResourceLocation> rejectedEnchantments,
            @Nullable Boolean expectedAnvil,
            String message
    ) {
        assertExtraEnchantments(helper, stack, rejectedEnchantments, false, false, false, false, expectedAnvil, message);
    }

    static void assertExtraEnchantments(
            GameTestHelper helper,
            ItemStack stack,
            Set<ResourceLocation> enchantmentIds,
            boolean expectedPrimary,
            boolean expectedSupported,
            boolean expectedDefinitionSupport,
            boolean expectedBook,
            @Nullable Boolean expectedAnvil,
            String message
    ) {
        var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (var enchantmentId : enchantmentIds) {
            assertSingleEnchantmentSurfaces(
                    helper,
                    stack,
                    enchantmentLookup.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, enchantmentId)),
                    expectedPrimary,
                    expectedSupported,
                    expectedDefinitionSupport,
                    expectedBook,
                    expectedAnvil,
                    message
            );
        }
    }

    static void assertSingleEnchantmentSurfaces(
            GameTestHelper helper,
            ItemStack stack,
            Holder<Enchantment> enchantment,
            boolean expectedPrimary,
            boolean expectedSupported,
            boolean expectedDefinitionSupport,
            boolean expectedBook,
            Boolean expectedAnvil,
            String message
    ) {
        var item = stack.getItem();
        var enchantmentId = enchantment.unwrapKey().orElseThrow().location();

        helper.assertTrue(item.isPrimaryItemFor(stack, enchantment) == expectedPrimary,
                message + " primary rule changed for " + enchantmentId + ": expected " + expectedPrimary);
        helper.assertTrue(item.supportsEnchantment(stack, enchantment) == expectedSupported,
                message + " supported rule changed for " + enchantmentId + ": expected " + expectedSupported);
        helper.assertTrue(enchantment.value().canEnchant(stack) == expectedDefinitionSupport,
                message + " definition support changed for " + enchantmentId + ": expected " + expectedDefinitionSupport);
        helper.assertTrue(item.isBookEnchantable(stack, createEnchantedBook(enchantment)) == expectedBook,
                message + " book rule changed for " + enchantmentId + ": expected " + expectedBook);

        if (expectedAnvil != null) {
            helper.assertTrue(item instanceof NonDamageableAnvilMergeItem,
                    message + " anvil expectation requires NonDamageableAnvilMergeItem");
            var actualAnvil = ((NonDamageableAnvilMergeItem) item).isAnvilMergeEnchantmentAllowed(stack, enchantment);
            helper.assertTrue(actualAnvil == expectedAnvil,
                    message + " anvil rule changed for " + enchantmentId + ": expected " + expectedAnvil);
        }
    }

    public static List<ItemStack> getRegisteredItemStacks(Predicate<Item> itemPredicate) {
        return ItemRegistry.ITEMS.getEntries().stream()
                .map(DeferredHolder::get)
                .filter(itemPredicate)
                .sorted(Comparator.comparing(item -> String.valueOf(BuiltInRegistries.ITEM.getKey(item))))
                .map(ItemStack::new)
                .toList();
    }

    static Set<ResourceLocation> expectedSpellGunEnchantments(ItemStack stack) {
        var expectedEnchantments = registryIdSet(
                Enchantments.ALACRITY,
                Enchantments.REFLUX,
                Enchantments.RESERVOIR,
                Enchantments.SURGE,
                Enchantments.ATTUNEMENT,
                Enchantments.TENSE,
                Enchantments.WISDOM,
                Enchantments.PLUNDER
        );
        addExpectedMalumSpiritPlunderIfPresent(stack, expectedEnchantments);
        return expectedEnchantments;
    }

    public static Set<ResourceLocation> expectedOffhandEnchantments() {
        return registryIdSet(
                Enchantments.ALACRITY,
                Enchantments.REFLUX,
                Enchantments.RESERVOIR,
                Enchantments.SURGE,
                Enchantments.ATTUNEMENT,
                Enchantments.TENSE
        );
    }

    public static Set<ResourceLocation> expectedEnchantedCircletEnchantments() {
        var expectedEnchantments = new LinkedHashSet<>(expectedOffhandEnchantments());
        expectedEnchantments.addAll(registryIdSet(Enchantments.WISDOM));
        return expectedEnchantments;
    }

    static Set<ResourceLocation> expectedReferencePrimaryEnchantments(
            RegistryAccess registryAccess,
            ItemStack referenceStack,
            Set<ResourceLocation> requiredExtraEnchantments
    ) {
        return expectedReferenceEnchantments(
                registryAccess,
                requiredExtraEnchantments,
                enchantment -> referenceStack.getItem().isPrimaryItemFor(referenceStack, enchantment)
        );
    }

    static Set<ResourceLocation> expectedReferenceSupportedEnchantments(
            RegistryAccess registryAccess,
            ItemStack referenceStack,
            Set<ResourceLocation> requiredExtraEnchantments
    ) {
        return expectedReferenceEnchantments(
                registryAccess,
                requiredExtraEnchantments,
                enchantment -> referenceStack.getItem().supportsEnchantment(referenceStack, enchantment)
        );
    }

    static Set<ResourceLocation> expectedReferenceDefinitionEnchantments(
            RegistryAccess registryAccess,
            ItemStack referenceStack,
            Set<ResourceLocation> requiredExtraEnchantments
    ) {
        return expectedReferenceEnchantments(
                registryAccess,
                requiredExtraEnchantments,
                enchantment -> enchantment.value().canEnchant(referenceStack)
        );
    }

    static Set<ResourceLocation> expectedReferenceBookEnchantments(
            RegistryAccess registryAccess,
            ItemStack referenceStack,
            Set<ResourceLocation> requiredExtraEnchantments
    ) {
        return expectedReferenceEnchantments(
                registryAccess,
                requiredExtraEnchantments,
                enchantment -> referenceStack.getItem().isBookEnchantable(referenceStack, createEnchantedBook(enchantment))
        );
    }

    static Set<ResourceLocation> expectedReferenceEnchantments(
            RegistryAccess registryAccess,
            Set<ResourceLocation> requiredExtraEnchantments,
            Predicate<Holder<Enchantment>> predicate
    ) {
        var expectedEnchantments = collectAllowedEnchantments(registryAccess, predicate);
        expectedEnchantments.addAll(requiredExtraEnchantments);
        return expectedEnchantments;
    }

    public static Set<ResourceLocation> expectedRightClickMagicWeaponEnchantments(RegistryAccess registryAccess, ItemStack stack) {
        if (stack.is(ItemRegistry.SMASHCAST_SCEPTER.get())) {
            var expectedEnchantments = collectAllowedEnchantments(
                    registryAccess,
                    enchantment -> enchantment.value().canEnchant(new ItemStack(Items.MACE))
                            && !isDurabilityTargetEnchantment(enchantment)
            );
            expectedEnchantments.addAll(collectAllowedEnchantments(
                    registryAccess,
                    enchantment -> enchantment.value().isSupportedItem(stack)
            ));
            expectedEnchantments.addAll(registryIdSet(
                    Enchantments.WISDOM,
                    Enchantments.PLUNDER
            ));
            addExpectedMalumMagicCapableWeaponEnchantmentsIfPresent(stack, expectedEnchantments);
            addExpectedMalumSpiritPlunderIfPresent(stack, expectedEnchantments);
            addExpectedMalumReplenishingIfPresent(stack, expectedEnchantments);
            return expectedEnchantments;
        }

        var expectedEnchantments = collectAllowedEnchantments(
                registryAccess,
                enchantment -> enchantment.value().canEnchant(new ItemStack(Items.DIAMOND_SWORD))
                        && !isDurabilityTargetEnchantment(enchantment)
        );
        expectedEnchantments.addAll(registryIdSet(
                Enchantments.WISDOM
        ));
        if (stack.is(ItemRegistry.REVOLVERCAST_STAFF.get())) {
            expectedEnchantments.add(Enchantments.TRANSCENDENCE.location());
        }
        if (stack.getItem() instanceof AttributeEnchantmentPolicy policy) {
            for (var type : policy.directlyApplicableAttributeEnchantments()) {
                expectedEnchantments.add(type.enchantmentKey().location());
            }
        }
        addExpectedMalumSpiritPlunderIfPresent(stack, expectedEnchantments);
        addExpectedMalumMagicCapableWeaponEnchantmentsIfPresent(stack, expectedEnchantments);
        addExpectedMalumReplenishingIfPresent(stack, expectedEnchantments);
        return expectedEnchantments;
    }

    public static Set<ResourceLocation> expectedChargedTwinBladeStaffEnchantments(RegistryAccess registryAccess, ItemStack stack) {
        var expectedEnchantments = collectAllowedEnchantments(
                registryAccess,
                enchantment -> enchantment.value().canEnchant(new ItemStack(Items.DIAMOND_SWORD))
                        && !isDurabilityTargetEnchantment(enchantment)
        );
        expectedEnchantments.addAll(collectAllowedEnchantments(
                registryAccess,
                enchantment -> enchantment.value().canEnchant(new ItemStack(Items.TRIDENT))
                        && !isDurabilityTargetEnchantment(enchantment)
        ));
        expectedEnchantments.addAll(registryIdSet(
                Enchantments.WISDOM
        ));
        addExpectedMalumMagicCapableWeaponEnchantmentsIfPresent(stack, expectedEnchantments);
        addExpectedMalumSpiritPlunderIfPresent(stack, expectedEnchantments);
        return expectedEnchantments;
    }

    public static Set<ResourceLocation> expectedCircuitHeatStaffEnchantments(RegistryAccess registryAccess, ItemStack stack) {
        var expectedEnchantments = collectAllowedEnchantments(
                registryAccess,
                enchantment -> enchantment.value().canEnchant(new ItemStack(Items.DIAMOND_SWORD))
                        && !isDurabilityTargetEnchantment(enchantment)
        );
        expectedEnchantments.addAll(registryIdSet(
                Enchantments.WISDOM
        ));
        addExpectedMalumMagicCapableWeaponEnchantmentsIfPresent(stack, expectedEnchantments);
        addExpectedMalumSpiritPlunderIfPresent(stack, expectedEnchantments);
        return expectedEnchantments;
    }

    public static Set<ResourceLocation> expectedSpellSideEdgeEnchantments(RegistryAccess registryAccess, ItemStack stack) {
        var expectedEnchantments = collectAllowedEnchantments(
                registryAccess,
                enchantment -> enchantment.value().canEnchant(new ItemStack(Items.DIAMOND_SWORD))
        );
        expectedEnchantments.addAll(registryIdSet(
                Enchantments.WISDOM,
                Enchantments.ALACRITY,
                Enchantments.REFLUX,
                Enchantments.RESERVOIR,
                Enchantments.TENSE
        ));
        addExpectedMalumMagicCapableWeaponEnchantmentsIfPresent(stack, expectedEnchantments);
        addExpectedMalumSpiritPlunderIfPresent(stack, expectedEnchantments);
        return expectedEnchantments;
    }

    static Set<ResourceLocation> expectedMultipurposeStaffrifleEnchantments(ItemStack stack) {
        var expectedEnchantments = registryIdSet(
                Enchantments.ALACRITY,
                Enchantments.REFLUX,
                Enchantments.RESERVOIR,
                Enchantments.TRANSCENDENCE,
                Enchantments.TENSE,
                Enchantments.WISDOM,
                Enchantments.PLUNDER
        );
        addExpectedMalumSpiritPlunderIfPresent(stack, expectedEnchantments);
        return expectedEnchantments;
    }

    static Set<ResourceLocation> expectedReflectcastShieldEnchantments(RegistryAccess registryAccess, ItemStack stack) {
        var expectedEnchantments = collectAllowedEnchantments(
                registryAccess,
                enchantment -> enchantment.value().canEnchant(new ItemStack(Items.SHIELD))
        );
        expectedEnchantments.addAll(registryIdSet(
                Enchantments.WISDOM
        ));
        return expectedEnchantments;
    }

    static Set<ResourceLocation> requiredElementalBowExtraEnchantments() {
        return registryIdSet(
                Enchantments.TRANSCENDENCE,
                Enchantments.WISDOM,
                Enchantments.PLUNDER,
                Enchantments.SYNTHESIS
        );
    }

    static Set<ResourceLocation> requiredStaffExtraEnchantments() {
        return registryIdSet(
                Enchantments.WISDOM
        );
    }

    static Set<ResourceLocation> staffMiningEnchantments() {
        return registryIdSet(
                net.minecraft.world.item.enchantment.Enchantments.FORTUNE,
                net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH
        );
    }

    static Set<ResourceLocation> rejectedPastelStaffExtraEnchantments() {
        return registryIdSet(
                Enchantments.TRANSCENDENCE
        );
    }

    static Set<ResourceLocation> requiredMalumMagicCapableWeaponEnchantments() {
        return Set.of(MALUM_HAUNTED, MALUM_ANIMATED);
    }

    static Set<ResourceLocation> expectedFlaskEnchantments() {
        return registryIdSet(
                Enchantments.GUZZLE,
                Enchantments.LARGE_MUG,
                Enchantments.RED_ENERGY,
                Enchantments.GLOW_ENERGY
        );
    }

    static Set<ResourceLocation> expectedAlchemistsFlaskEnchantments() {
        return registryIdSet(
                Enchantments.LARGE_MUG,
                Enchantments.RED_ENERGY,
                Enchantments.GLOW_ENERGY
        );
    }

    static Set<ResourceLocation> expectedRandomBookLootEnchantments() {
        return registryIdSet(
                Enchantments.ALACRITY,
                Enchantments.REFLUX,
                Enchantments.RESERVOIR,
                Enchantments.SURGE,
                Enchantments.ATTUNEMENT,
                Enchantments.TENSE,
                Enchantments.TRANSCENDENCE,
                Enchantments.WISDOM,
                Enchantments.PLUNDER
        );
    }

    static Set<ResourceLocation> expectedEnchantressRobeEnchantments(RegistryAccess registryAccess, ItemStack stack) {
        var probeStack = createArmorProbeStack(stack);
        var expectedEnchantments = collectAllowedEnchantments(
                registryAccess,
                enchantment -> enchantment.value().canEnchant(probeStack)
        );
        expectedEnchantments.addAll(registryIdSet(Enchantments.WISDOM));
        return expectedEnchantments;
    }

    static Set<ResourceLocation> expectedStealthRuneArmorEnchantments(RegistryAccess registryAccess, ItemStack stack) {
        var probeStack = createArmorProbeStack(stack);
        var expectedEnchantments = collectAllowedEnchantments(
                registryAccess,
                enchantment -> enchantment.value().canEnchant(probeStack)
        );
        expectedEnchantments.addAll(registryIdSet(Enchantments.WISDOM));
        return expectedEnchantments;
    }

    static Set<ResourceLocation> expectedChromaticMagiaDressEnchantments(RegistryAccess registryAccess, ItemStack stack) {
        var probeStack = createArmorProbeStack(stack);
        var expectedEnchantments = collectAllowedEnchantments(
                registryAccess,
                enchantment -> enchantment.value().canEnchant(probeStack)
        );
        expectedEnchantments.addAll(registryIdSet(Enchantments.WISDOM));
        return expectedEnchantments;
    }

    static Set<ResourceLocation> expectedElementMaidenRobeEnchantments(RegistryAccess registryAccess, ItemStack stack) {
        var probeStack = createArmorProbeStack(stack);
        var expectedEnchantments = collectAllowedEnchantments(
                registryAccess,
                enchantment -> enchantment.value().canEnchant(probeStack)
        );
        expectedEnchantments.addAll(registryIdSet(Enchantments.WISDOM));
        if (stack.getItem() instanceof ElementMaidenRobeItem robeItem && robeItem.hasImbueSlot()) {
            expectedEnchantments.addAll(registryIdSet(
                    Enchantments.SURGE,
                    Enchantments.ATTUNEMENT
            ));
        }
        return expectedEnchantments;
    }

    static Set<ResourceLocation> expectedMagiAgentSuitEnchantments(RegistryAccess registryAccess, ItemStack stack) {
        var probeStack = createArmorProbeStack(stack);
        var expectedEnchantments = collectAllowedEnchantments(
                registryAccess,
                enchantment -> enchantment.value().canEnchant(probeStack)
        );
        expectedEnchantments.addAll(registryIdSet(Enchantments.WISDOM));
        return expectedEnchantments;
    }

    static ItemStack createArmorProbeStack(ItemStack stack) {
        if (!(stack.getItem() instanceof ArmorItem armorItem)) {
            throw new IllegalArgumentException("Expected armor item for enchantment probe: " + stack);
        }

        return switch (armorItem.getType()) {
            case HELMET -> new ItemStack(Items.LEATHER_HELMET);
            case CHESTPLATE -> new ItemStack(Items.LEATHER_CHESTPLATE);
            case LEGGINGS -> new ItemStack(Items.LEATHER_LEGGINGS);
            case BOOTS -> new ItemStack(Items.LEATHER_BOOTS);
            default -> throw new IllegalArgumentException("Unsupported armor type for enchantment probe: " + armorItem.getType());
        };
    }

    @SafeVarargs
    public static Set<ResourceLocation> registryIdSet(ResourceKey<Enchantment>... enchantments) {
        var ids = new LinkedHashSet<ResourceLocation>();
        for (var enchantment : enchantments) {
            ids.add(enchantment.location());
        }
        return ids;
    }

    static Set<ResourceLocation> collectAllowedEnchantments(
            RegistryAccess registryAccess,
            Predicate<Holder<Enchantment>> predicate
    ) {
        var allowedEnchantments = new LinkedHashSet<ResourceLocation>();
        var enchantments = registryAccess.lookupOrThrow(Registries.ENCHANTMENT).listElements()
                .sorted(Comparator.comparing(holder -> holder.key().location().toString()))
                .toList();

        for (var enchantment : enchantments) {
            if (predicate.test(enchantment)) {
                allowedEnchantments.add(enchantment.key().location());
            }
        }
        return allowedEnchantments;
    }

    public static Set<ResourceLocation> allRegisteredEnchantmentIds(RegistryAccess registryAccess) {
        return collectAllowedEnchantments(registryAccess, enchantment -> true);
    }

    public static ItemStack createEnchantedBook(Holder<Enchantment> enchantment) {
        return createEnchantedBook(enchantment, 1);
    }

    public static ItemStack createEnchantedBook(Holder<Enchantment> enchantment, int level) {
        var book = new ItemStack(Items.ENCHANTED_BOOK);
        book.enchant(enchantment, level);
        return book;
    }

    public static ItemStack createEnchantedBook(BookEnchantment... enchantments) {
        var book = new ItemStack(Items.ENCHANTED_BOOK);
        for (var enchantment : enchantments) {
            book.enchant(enchantment.enchantment(), enchantment.level());
        }
        return book;
    }

    public static int getEnchantmentLevel(ItemStack stack, Holder<Enchantment> enchantment) {
        return EnchantmentHelper.getEnchantmentsForCrafting(stack).getLevel(enchantment);
    }

    private record BookEnchantment(
            Holder<Enchantment> enchantment,
            int level
    ) {
    }

    static boolean isDurabilityTargetEnchantment(Holder<Enchantment> enchantment) {
        return enchantment.value().canEnchant(new ItemStack(Items.ELYTRA));
    }

    static void addExpectedMalumSpiritPlunderIfPresent(ItemStack stack, Set<ResourceLocation> expectedEnchantments) {
        if (ModList.get().isLoaded(MALUM_MOD_ID) && stack.is(MALUM_SOUL_SHATTER_CAPABLE_WEAPON)) {
            expectedEnchantments.add(MALUM_SPIRIT_PLUNDER);
        }
    }

    static void addExpectedMalumReplenishingIfPresent(ItemStack stack, Set<ResourceLocation> expectedEnchantments) {
        if (ModList.get().isLoaded(MALUM_MOD_ID)
                && stack.is(TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "enchantable/replenishing")))) {
            expectedEnchantments.add(MALUM_REPLENISHING);
        }
    }

    public static void addExpectedMalumMagicCapableWeaponEnchantmentsIfPresent(ItemStack stack, Set<ResourceLocation> expectedEnchantments) {
        if (ModList.get().isLoaded(MALUM_MOD_ID) && stack.is(MALUM_MAGIC_CAPABLE_WEAPON)) {
            expectedEnchantments.add(MALUM_HAUNTED);
            expectedEnchantments.add(MALUM_ANIMATED);
        }
    }

    static void assertMalumMagicCapableWeaponEnchantmentAccepted(
            GameTestHelper helper,
            ItemStack stack,
            Holder<Enchantment> enchantment,
            String itemName
    ) {
        var item = stack.getItem();
        helper.assertTrue(stack.is(MALUM_MAGIC_CAPABLE_WEAPON),
                itemName + " is missing malum:magic_capable_weapon");
        helper.assertTrue(item.isPrimaryItemFor(stack, enchantment),
                itemName + " should allow the Malum enchantment as a primary enchantment");
        helper.assertTrue(item.supportsEnchantment(stack, enchantment),
                itemName + " should support the Malum enchantment");
        helper.assertTrue(enchantment.value().canEnchant(stack),
                itemName + " should pass the enchantment definition item tag");
        helper.assertTrue(item.isBookEnchantable(stack, createEnchantedBook(enchantment)),
                itemName + " should accept the Malum enchantment from enchanted books");
        if (item instanceof NonDamageableAnvilMergeItem mergeItem) {
            helper.assertTrue(mergeItem.isAnvilMergeEnchantmentAllowed(stack, enchantment),
                    itemName + " should accept the Malum enchantment through anvil merges");
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

    static void assertApprenticeEnchantmentFlags(
            GameTestHelper helper,
            ResourceKey<Enchantment> enchantmentKey,
            boolean expectedTreasureOnly,
            boolean expectedInEnchantingTable,
            boolean expectedTradeable,
            boolean expectedOnRandomLoot
    ) {
        var enchantment = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantmentKey);
        var enchantmentId = enchantmentKey.location();
        var actualTreasureOnly = enchantment.is(EnchantmentTags.TREASURE);
        var actualInEnchantingTable = enchantment.is(EnchantmentTags.IN_ENCHANTING_TABLE);
        var actualTradeable = enchantment.is(EnchantmentTags.TRADEABLE);
        var actualOnRandomLoot = enchantment.is(EnchantmentTags.ON_RANDOM_LOOT);
        helper.assertTrue(actualTreasureOnly == expectedTreasureOnly,
                "Treasure tag changed for " + enchantmentId + ": expected " + expectedTreasureOnly + " but got " + actualTreasureOnly);
        helper.assertTrue(actualInEnchantingTable == expectedInEnchantingTable,
                "Enchanting-table tag changed for " + enchantmentId + ": expected " + expectedInEnchantingTable + " but got " + actualInEnchantingTable);
        helper.assertTrue(actualTradeable == expectedTradeable,
                "Tradeable tag changed for " + enchantmentId + ": expected " + expectedTradeable + " but got " + actualTradeable);
        helper.assertTrue(actualOnRandomLoot == expectedOnRandomLoot,
                "Random-loot tag changed for " + enchantmentId + ": expected " + expectedOnRandomLoot + " but got " + actualOnRandomLoot);
    }

    static List<ResourceKey<Enchantment>> getRegisteredEnchantments() {
        return List.of(
                Enchantments.REFLUX,
                Enchantments.RESERVOIR,
                Enchantments.ALACRITY,
                Enchantments.TENSE,
                Enchantments.SURGE,
                Enchantments.ATTUNEMENT,
                Enchantments.TRANSCENDENCE,
                Enchantments.WISDOM,
                Enchantments.PLUNDER,
                Enchantments.GUZZLE,
                Enchantments.LARGE_MUG,
                Enchantments.RED_ENERGY,
                Enchantments.GLOW_ENERGY,
                Enchantments.SYNTHESIS
        );
    }
}
