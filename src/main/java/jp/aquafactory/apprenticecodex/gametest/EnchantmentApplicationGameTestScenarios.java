package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentPolicy;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentType;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.enchantment.PlunderTarget;
import jp.aquafactory.apprenticecodex.enchantment.TranscendencePolicy;
import jp.aquafactory.apprenticecodex.enchantment.WisdomPolicy;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 1.21.1 の enchantment definition / item tag を正とし、Java 側ポリシーとの境界を固定する。
 */
final class EnchantmentApplicationGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private static final Set<AttributeEnchantmentType> GENERAL_STAFF_AND_ARMOR_ENCHANTMENTS = Set.of(
            AttributeEnchantmentType.ALACRITY,
            AttributeEnchantmentType.REFLUX,
            AttributeEnchantmentType.RESERVOIR,
            AttributeEnchantmentType.TENSE
    );
    private static final Set<AttributeEnchantmentType> BULWARK_ENCHANTMENTS = Set.of(
            AttributeEnchantmentType.REFLUX,
            AttributeEnchantmentType.RESERVOIR
    );

    private EnchantmentApplicationGameTestScenarios() {
    }

    static void itemSurfacesKeepExpectedMatrix(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertDefinitionSurface(helper, new ItemStack(ItemRegistry.IRON_SPELLCASTER_GUN.get()),
                    AttributeEnchantmentPolicy.ALL_ATTRIBUTE_ENCHANTMENTS, "Spell Gun");
            assertDefinitionSurface(helper, new ItemStack(ItemRegistry.COPPER_SWINGCAST_STAFF.get()),
                    GENERAL_STAFF_AND_ARMOR_ENCHANTMENTS, "Swingcast Staff");
            assertDefinitionSurface(helper, new ItemStack(ItemRegistry.MITHRIL_FREECAST_STAFF.get()),
                    GENERAL_STAFF_AND_ARMOR_ENCHANTMENTS, "Mithril Freecast Staff");
            assertDefinitionSurface(helper, new ItemStack(ItemRegistry.REVOLVERCAST_STAFF.get()),
                    GENERAL_STAFF_AND_ARMOR_ENCHANTMENTS, "Revolvercast Staff");
            assertDefinitionSurface(helper, new ItemStack(ItemRegistry.STEALTH_RUNE_ARMOR_HEAD.get()),
                    GENERAL_STAFF_AND_ARMOR_ENCHANTMENTS, "Stealth Rune Armor");
            assertDefinitionSurface(helper, new ItemStack(ItemRegistry.BULWARK_GREATSHIELD.get()),
                    BULWARK_ENCHANTMENTS, "Bulwark Greatshield");
            assertDefinitionSurface(helper, new ItemStack(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get()),
                    Set.of(
                            AttributeEnchantmentType.ALACRITY,
                            AttributeEnchantmentType.REFLUX,
                            AttributeEnchantmentType.RESERVOIR,
                            AttributeEnchantmentType.SURGE,
                            AttributeEnchantmentType.TENSE
                    ), "Multipurpose Staffrifle");
            assertDefinitionSurface(helper, new ItemStack(ItemRegistry.MANA_FORCE_BLADE.get()),
                    Set.of(AttributeEnchantmentType.SURGE, AttributeEnchantmentType.ATTUNEMENT),
                    "Mana Force Blade");
            assertStaffEnchantmentSurfaces(helper);
        });
    }

    static void directApplicationPoliciesKeepExpectedMatrix(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertEnchantableTagIds(helper);
            assertDirectAttributePolicy(helper, ItemRegistry.IRON_SPELLCASTER_GUN.get(),
                    AttributeEnchantmentPolicy.ALL_ATTRIBUTE_ENCHANTMENTS);
            assertDirectAttributePolicy(helper, ItemRegistry.COPPER_SWINGCAST_STAFF.get(),
                    GENERAL_STAFF_AND_ARMOR_ENCHANTMENTS);
            assertDirectAttributePolicy(helper, ItemRegistry.MITHRIL_FREECAST_STAFF.get(),
                    GENERAL_STAFF_AND_ARMOR_ENCHANTMENTS);
            assertDirectAttributePolicy(helper, ItemRegistry.REVOLVERCAST_STAFF.get(),
                    GENERAL_STAFF_AND_ARMOR_ENCHANTMENTS);
            assertDirectAttributePolicy(helper, ItemRegistry.STEALTH_RUNE_ARMOR_HEAD.get(),
                    GENERAL_STAFF_AND_ARMOR_ENCHANTMENTS);
            assertDirectAttributePolicy(helper, ItemRegistry.BULWARK_GREATSHIELD.get(), BULWARK_ENCHANTMENTS);
            assertDirectAttributePolicy(helper, ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get(), Set.of(
                    AttributeEnchantmentType.ALACRITY,
                    AttributeEnchantmentType.REFLUX,
                    AttributeEnchantmentType.RESERVOIR,
                    AttributeEnchantmentType.SURGE,
                    AttributeEnchantmentType.TENSE
            ));
            assertDirectAttributePolicy(helper, ItemRegistry.MANA_FORCE_BLADE.get(), Set.of(
                    AttributeEnchantmentType.SURGE,
                    AttributeEnchantmentType.ATTUNEMENT
            ));

            helper.assertTrue(WisdomPolicy.supportsDirectApplication(ItemRegistry.FOCUS_STAFFBOW.get()),
                    "Focus Staffbow should keep its Wisdom surface");
            helper.assertTrue(PlunderTarget.supportsDirectApplication(ItemRegistry.FOCUS_STAFFBOW.get()),
                    "Focus Staffbow should keep its Plunder surface");
            helper.assertTrue(PlunderTarget.supportsDirectApplication(ItemRegistry.SMASHCAST_SCEPTER.get()),
                    "Smashcast Scepter should keep its Plunder surface");

            assertDirectPoliciesReachApplicationSurfaces(helper);
        });
    }

    private static void assertEnchantableTagIds(GameTestHelper helper) {
        for (var entry : java.util.List.of(
                java.util.Map.entry(Enchantments.ALACRITY_ENCHANTABLE, Enchantments.ALACRITY),
                java.util.Map.entry(Enchantments.REFLUX_ENCHANTABLE, Enchantments.REFLUX),
                java.util.Map.entry(Enchantments.RESERVOIR_ENCHANTABLE, Enchantments.RESERVOIR),
                java.util.Map.entry(Enchantments.SURGE_ENCHANTABLE, Enchantments.SURGE),
                java.util.Map.entry(Enchantments.ATTUNEMENT_ENCHANTABLE, Enchantments.ATTUNEMENT),
                java.util.Map.entry(Enchantments.TENSE_ENCHANTABLE, Enchantments.TENSE),
                java.util.Map.entry(Enchantments.TRANSCENDENCE_ENCHANTABLE, Enchantments.TRANSCENDENCE),
                java.util.Map.entry(Enchantments.WISDOM_ENCHANTABLE, Enchantments.WISDOM),
                java.util.Map.entry(Enchantments.PLUNDER_ENCHANTABLE, Enchantments.PLUNDER)
        )) {
            assertEnchantableTagId(helper, entry.getKey(), entry.getValue());
        }
    }

    private static void assertEnchantableTagId(
            GameTestHelper helper,
            TagKey<Item> tag,
            ResourceKey<Enchantment> enchantment
    ) {
        var enchantmentId = enchantment.location();
        var expected = ResourceLocation.fromNamespaceAndPath(
                enchantmentId.getNamespace(),
                enchantmentId.getPath() + "_enchantable"
        );
        helper.assertTrue(tag.location().equals(expected),
                "Enchantable tag id should match the 1.21.1 contract: expected="
                        + expected + " actual=" + tag.location());
    }

    static void specialApplicationRulesStayExplicit(GameTestHelper helper) {
        helper.succeedIf(() -> {
            helper.assertFalse(ItemRegistry.CIRCUIT_HEAT_STAFF.get() instanceof PlunderTarget,
                    "Circuit Heat Staff must not convert Plunder into Looting");
            helper.assertFalse(ItemRegistry.ENCHANTRESS_ROBE.get() instanceof TranscendencePolicy,
                    "Enchantress Robe must reject and ignore Transcendence");
            helper.assertTrue(TranscendencePolicy.supportsDirectApplication(ItemRegistry.STEALTH_RUNE_ARMOR_BODY.get()),
                    "Stealth Rune chest armor should accept Transcendence");
            helper.assertFalse(TranscendencePolicy.supportsDirectApplication(ItemRegistry.STEALTH_RUNE_ARMOR_HEAD.get()),
                    "Stealth Rune non-chest armor should reject Transcendence");

            var scrollcaster = ItemRegistry.SCROLLCASTER_GAUNTLET.get();
            helper.assertFalse(TranscendencePolicy.supportsDirectApplication(scrollcaster),
                    "Scrollcaster Gauntlet enchantments are synchronized through calibration");
            helper.assertFalse(WisdomPolicy.supportsDirectApplication(scrollcaster),
                    "Scrollcaster Gauntlet Wisdom is synchronized through calibration");

            helper.assertTrue(TranscendencePolicy.supportsDirectApplication(ItemRegistry.REVOLVERCAST_STAFF.get()),
                    "Revolvercast Staff should accept Transcendence like the 1.20.1 implementation");
            var elementalBow = (TranscendencePolicy) ItemRegistry.ELEMENTAL_BOW.get();
            helper.assertTrue(elementalBow.transcendenceHandling() == TranscendencePolicy.Handling.INTERNAL,
                    "Elemental Bow should keep internal Transcendence handling");
        });
    }

    static void acquisitionFlagsKeepExpectedValues(GameTestHelper helper) {
        helper.succeedIf(() -> {
            for (var enchantment : Set.of(
                    Enchantments.ALACRITY, Enchantments.REFLUX, Enchantments.RESERVOIR,
                    Enchantments.SURGE, Enchantments.ATTUNEMENT, Enchantments.TENSE,
                    Enchantments.WISDOM, Enchantments.PLUNDER
            )) {
                assertApprenticeEnchantmentFlags(helper, enchantment, false, true, true, true);
            }
            assertApprenticeEnchantmentFlags(helper, Enchantments.TRANSCENDENCE, true, false, true, true);
            for (var enchantment : Set.of(
                    Enchantments.GUZZLE, Enchantments.LARGE_MUG, Enchantments.RED_ENERGY,
                    Enchantments.GLOW_ENERGY, Enchantments.SYNTHESIS, Enchantments.SHELL,
                    Enchantments.SYNCHRONIZATION, Enchantments.NEUTRALIZATION
            )) {
                assertApprenticeEnchantmentFlags(helper, enchantment, false, true, false, false);
            }
        });
    }

    private static void assertDefinitionSurface(
            GameTestHelper helper,
            ItemStack stack,
            Set<AttributeEnchantmentType> expected,
            String itemName
    ) {
        var enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (var type : AttributeEnchantmentType.values()) {
            var actual = enchantments.getOrThrow(type.enchantmentKey()).value().canEnchant(stack);
            helper.assertTrue(actual == expected.contains(type),
                    itemName + " definition surface changed for " + type + ": " + actual);
        }
    }

    private static void assertStaffEnchantmentSurfaces(GameTestHelper helper) {
        var reference = new ItemStack(ItemRegistry.PASTEL_STAFF.get());
        var referenceDefinition = collectEnchantments(helper, reference,
                (stack, enchantment) -> enchantment.value().canEnchant(stack));
        var referenceBooks = collectEnchantments(helper, reference,
                (stack, enchantment) -> stack.supportsEnchantment(enchantment));
        var referenceTable = collectEnchantments(helper, reference,
                (stack, enchantment) -> stack.getItem().isPrimaryItemFor(stack, enchantment));

        for (var entry : java.util.List.of(
                java.util.Map.entry("Pastel Staff", reference),
                java.util.Map.entry("Multicast Echo Staff", new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get())),
                java.util.Map.entry("Zenith Staff", new ItemStack(ItemRegistry.ZENITH_STAFF.get())),
                java.util.Map.entry("Circuit Heat Staff", new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get()))
        )) {
            var stack = entry.getValue();
            helper.assertTrue(referenceDefinition.equals(collectEnchantments(helper, stack,
                            (target, enchantment) -> enchantment.value().canEnchant(target))),
                    entry.getKey() + " definition surface should match Pastel Staff");
            helper.assertTrue(referenceBooks.equals(collectEnchantments(helper, stack,
                            (target, enchantment) -> target.supportsEnchantment(enchantment))),
                    entry.getKey() + " enchanted-book surface should match Pastel Staff");
            helper.assertTrue(referenceTable.equals(collectEnchantments(helper, stack,
                            (target, enchantment) -> target.getItem().isPrimaryItemFor(target, enchantment))),
                    entry.getKey() + " enchanting-table surface should match Pastel Staff");

            assertVanillaEnchantment(helper, stack, "fortune", false, entry.getKey());
            assertVanillaEnchantment(helper, stack, "silk_touch", false, entry.getKey());
            assertVanillaEnchantment(helper, stack, "sharpness", true, entry.getKey());
            assertVanillaEnchantment(helper, stack, "looting", true, entry.getKey());
        }
    }

    private static Set<ResourceLocation> collectEnchantments(
            GameTestHelper helper,
            ItemStack stack,
            EnchantmentSurfacePredicate predicate
    ) {
        var enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var result = new LinkedHashSet<ResourceLocation>();
        enchantments.listElements().forEach(enchantment -> {
            if (predicate.test(stack, enchantment)) {
                result.add(enchantment.key().location());
            }
        });
        return result;
    }

    private static void assertVanillaEnchantment(
            GameTestHelper helper,
            ItemStack stack,
            String enchantmentPath,
            boolean expected,
            String itemName
    ) {
        var enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var enchantment = enchantments.getOrThrow(ResourceKey.create(
                Registries.ENCHANTMENT,
                ResourceLocation.withDefaultNamespace(enchantmentPath)
        ));
        helper.assertTrue(enchantment.value().canEnchant(stack) == expected,
                itemName + " definition rule changed for " + enchantmentPath);
        helper.assertTrue(stack.supportsEnchantment(enchantment) == expected,
                itemName + " enchanted-book rule changed for " + enchantmentPath);
        helper.assertTrue(stack.getItem().isPrimaryItemFor(stack, enchantment) == expected,
                itemName + " enchanting-table rule changed for " + enchantmentPath);
    }

    private static void assertDirectAttributePolicy(
            GameTestHelper helper,
            net.minecraft.world.item.Item item,
            Set<AttributeEnchantmentType> expected
    ) {
        helper.assertTrue(item instanceof AttributeEnchantmentPolicy,
                item + " should expose an attribute enchantment policy");
        var policy = (AttributeEnchantmentPolicy) item;
        for (var type : AttributeEnchantmentType.values()) {
            helper.assertTrue(policy.supportsDirectAttributeEnchantment(type) == expected.contains(type),
                    item + " direct attribute policy changed for " + type);
        }
    }

    private static void assertDirectPoliciesReachApplicationSurfaces(GameTestHelper helper) {
        var enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (var itemEntry : ItemRegistry.ITEMS.getEntries()) {
            var item = itemEntry.get();
            var stack = new ItemStack(item);
            if (TranscendencePolicy.supportsDirectApplication(item)) {
                assertApplicationSurface(helper, stack, enchantments.getOrThrow(Enchantments.TRANSCENDENCE));
            }
            if (WisdomPolicy.supportsDirectApplication(item)) {
                assertApplicationSurface(helper, stack, enchantments.getOrThrow(Enchantments.WISDOM));
            }
            if (PlunderTarget.supportsDirectApplication(item)) {
                assertApplicationSurface(helper, stack, enchantments.getOrThrow(Enchantments.PLUNDER));
            }
            for (var type : AttributeEnchantmentType.values()) {
                if (AttributeEnchantmentPolicy.supportsDirectApplication(item, type)) {
                    assertApplicationSurface(helper, stack, enchantments.getOrThrow(type.enchantmentKey()));
                }
            }
        }
    }

    private static void assertApplicationSurface(
            GameTestHelper helper,
            ItemStack stack,
            net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment
    ) {
        var item = stack.getItem();
        var enchantmentId = enchantment.unwrapKey().orElseThrow().location();
        helper.assertTrue(enchantment.value().canEnchant(stack),
                item + " is declared for " + enchantmentId + " but is absent from the definition tag");
        helper.assertTrue(item.supportsEnchantment(stack, enchantment),
                item + " is declared for " + enchantmentId + " but rejects normal book application");
        helper.assertTrue(item.isPrimaryItemFor(stack, enchantment),
                item + " is declared for " + enchantmentId + " but rejects enchanting-table application");
    }

    @FunctionalInterface
    private interface EnchantmentSurfacePredicate {
        boolean test(
                ItemStack stack,
                net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment
        );
    }
}
