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
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * 1.21.1 の enchantment definition / item tag を正とし、Java 側ポリシーとの境界を固定する。
 */
final class EnchantmentApplicationGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private EnchantmentApplicationGameTestScenarios() {
    }

    static void itemSurfacesKeepExpectedMatrix(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertDefinitionSurface(helper, new ItemStack(ItemRegistry.IRON_SPELLCASTER_GUN.get()),
                    Set.of(
                            AttributeEnchantmentType.REFLUX,
                            AttributeEnchantmentType.RESERVOIR,
                            AttributeEnchantmentType.SURGE,
                            AttributeEnchantmentType.ATTUNEMENT
                    ), "Spell Gun");
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
        });
    }

    static void directApplicationPoliciesKeepExpectedMatrix(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertDirectAttributePolicy(helper, ItemRegistry.IRON_SPELLCASTER_GUN.get(), Set.of(
                    AttributeEnchantmentType.REFLUX,
                    AttributeEnchantmentType.RESERVOIR,
                    AttributeEnchantmentType.SURGE,
                    AttributeEnchantmentType.ATTUNEMENT
            ));
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
                    "Focus Staffbow should keep its 1.21.1 Wisdom surface");
            helper.assertTrue(PlunderTarget.supportsDirectApplication(ItemRegistry.SMASHCAST_SCEPTER.get()),
                    "Smashcast Scepter should keep its Plunder surface");
        });
    }

    static void specialApplicationRulesStayExplicit(GameTestHelper helper) {
        helper.succeedIf(() -> {
            helper.assertFalse(ItemRegistry.CIRCUIT_HEAT_STAFF.get() instanceof PlunderTarget,
                    "Circuit Heat Staff must not convert Plunder into Looting");

            var scrollcaster = ItemRegistry.SCROLLCASTER_GAUNTLET.get();
            helper.assertFalse(TranscendencePolicy.supportsDirectApplication(scrollcaster),
                    "Scrollcaster Gauntlet enchantments are synchronized through calibration");
            helper.assertFalse(WisdomPolicy.supportsDirectApplication(scrollcaster),
                    "Scrollcaster Gauntlet Wisdom is synchronized through calibration");

            var revolvercast = (TranscendencePolicy) ItemRegistry.REVOLVERCAST_STAFF.get();
            helper.assertTrue(revolvercast.transcendenceHandling() == TranscendencePolicy.Handling.DISABLED,
                    "Revolvercast Staff should keep the 1.21.1 Transcendence rejection");
            var elementalBow = (TranscendencePolicy) ItemRegistry.ELEMENTAL_BOW.get();
            helper.assertTrue(elementalBow.transcendenceHandling() == TranscendencePolicy.Handling.INTERNAL,
                    "Elemental Bow should keep internal Transcendence handling");
        });
    }

    static void acquisitionFlagsKeepExpectedValues(GameTestHelper helper) {
        helper.succeedIf(() -> {
            for (var enchantment : Set.of(
                    Enchantments.ALACRITY,
                    Enchantments.REFLUX,
                    Enchantments.RESERVOIR,
                    Enchantments.SURGE,
                    Enchantments.ATTUNEMENT,
                    Enchantments.TENSE,
                    Enchantments.WISDOM,
                    Enchantments.PLUNDER
            )) {
                assertApprenticeEnchantmentFlags(helper, enchantment, false, true, true, true);
            }
            assertApprenticeEnchantmentFlags(helper, Enchantments.TRANSCENDENCE, true, false, true, true);
            for (var enchantment : Set.of(
                    Enchantments.GUZZLE,
                    Enchantments.LARGE_MUG,
                    Enchantments.RED_ENERGY,
                    Enchantments.GLOW_ENERGY,
                    Enchantments.SYNTHESIS,
                    Enchantments.SHELL,
                    Enchantments.SYNCHRONIZATION,
                    Enchantments.NEUTRALIZATION
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
}
