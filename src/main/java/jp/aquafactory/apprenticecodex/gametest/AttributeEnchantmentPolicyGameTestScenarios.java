package jp.aquafactory.apprenticecodex.gametest;

import com.google.common.collect.ImmutableMultimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentResolver;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentType;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.MagicAttributeModifierHelper;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.UUID;

final class AttributeEnchantmentPolicyGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private static final double EPSILON = 1.0e-9D;

    private AttributeEnchantmentPolicyGameTestScenarios() {
    }

    static void forcedEnchantmentsCoexistAndKeepRawPositiveLevels(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = createInitializedPresetStack(ItemRegistry.COPPER_SPELL_AMPLIFIER.get());
            var level = 7;
            for (var type : AttributeEnchantmentType.values()) {
                var enchantment = ForgeRegistries.ENCHANTMENTS.getValue(type.enchantmentId());
                helper.assertTrue(enchantment != null,
                        "Attribute enchantment was not registered: " + type.enchantmentId());
                stack.enchant(enchantment, level);
            }

            var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
            helper.assertTrue(AttributeEnchantmentResolver.addModifiers(builder, stack, "gametest.forced_all"),
                    "Force-applied attribute enchantments should produce modifiers");
            var modifiers = builder.build();
            for (var type : AttributeEnchantmentType.values()) {
                var attribute = type.resolveAttribute(stack);
                helper.assertTrue(attribute != null,
                        "Initialized amplifier should resolve the attribute for " + type);
                assertSingleModifierAmount(
                        helper,
                        modifiers.get(attribute),
                        type.operation(),
                        type.amountPerLevel() * level,
                        "Force-applied enchantment should keep its raw positive level: " + type
                );
            }
        });
    }

    static void configuredAmountsPerLevelKeepDefaultsAndScaleLinearly(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var expectedDefaults = Map.of(
                    AttributeEnchantmentType.ALACRITY, 0.02D,
                    AttributeEnchantmentType.REFLUX, 0.05D,
                    AttributeEnchantmentType.RESERVOIR, 20.0D,
                    AttributeEnchantmentType.SURGE, 0.02D,
                    AttributeEnchantmentType.ATTUNEMENT, 0.03D,
                    AttributeEnchantmentType.TENSE, 0.04D
            );
            var level = 3;

            for (var type : AttributeEnchantmentType.values()) {
                var expectedDefault = expectedDefaults.get(type);
                helper.assertTrue(Math.abs(type.amountPerLevel() - expectedDefault) < EPSILON,
                        "Attribute enchantment default amount changed: " + type);

                var configuredAmount = expectedDefault + 0.013D;
                try (var ignored = ApprenticeCodexServerConfig
                        .useAttributeEnchantmentAmountPerLevelOverrideForGameTest(type, configuredAmount)) {
                    helper.assertTrue(Math.abs(type.amountPerLevel() - configuredAmount) < EPSILON,
                            "Attribute enchantment config override did not apply: " + type);

                    var stack = createInitializedPresetStack(ItemRegistry.COPPER_SPELL_AMPLIFIER.get());
                    var enchantment = ForgeRegistries.ENCHANTMENTS.getValue(type.enchantmentId());
                    helper.assertTrue(enchantment != null,
                            "Attribute enchantment was not registered: " + type.enchantmentId());
                    stack.enchant(enchantment, level);

                    var attribute = type.resolveAttribute(stack);
                    helper.assertTrue(attribute != null,
                            "Initialized amplifier should resolve the configured attribute for " + type);
                    var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
                    helper.assertTrue(AttributeEnchantmentResolver.addModifiers(
                                    builder, stack, "gametest.configured." + type.configKey()),
                            "Configured attribute enchantment should produce a modifier: " + type);
                    assertSingleModifierAmount(
                            helper,
                            builder.build().get(attribute),
                            type.operation(),
                            configuredAmount * level,
                            "Configured attribute enchantment should scale linearly: " + type
                    );
                }

                helper.assertTrue(Math.abs(type.amountPerLevel() - expectedDefault) < EPSILON,
                        "Attribute enchantment config override was not restored: " + type);
            }
        });
    }

    static void attunementResolutionFailureIsNoOp(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get());
            var attunement = ForgeRegistries.ENCHANTMENTS.getValue(AttributeEnchantmentType.ATTUNEMENT.enchantmentId());
            helper.assertTrue(attunement != null, "Attunement was not registered");
            stack.enchant(attunement, 5);

            var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
            helper.assertFalse(AttributeEnchantmentResolver.addModifiers(builder, stack, "gametest.attunement_noop"),
                    "Attunement should be a no-op when the imbued school cannot be resolved");
            helper.assertTrue(builder.build().isEmpty(),
                    "Failed Attunement resolution should not add a fallback spell power modifier");
        });
    }

    static void mergeHelperOnlyNormalizesLinearMagicAttributes(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var multiplyTotalId = UUID.fromString("9f9b92be-e70e-4310-8af7-b3375457be51");
            var vanillaId = UUID.fromString("b4fb9257-bfed-4688-80eb-7b55ac064a80");
            var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
            builder.put(AttributeRegistry.SPELL_POWER.get(), new AttributeModifier(
                    UUID.fromString("b46a3ca1-04f5-4e3f-ab70-a93406cedf84"), "magic_a", 0.10D,
                    AttributeModifier.Operation.MULTIPLY_BASE));
            builder.put(AttributeRegistry.SPELL_POWER.get(), new AttributeModifier(
                    UUID.fromString("29b18e79-b10a-4164-a376-dfa71c8310de"), "magic_b", 0.20D,
                    AttributeModifier.Operation.MULTIPLY_BASE));
            builder.put(AttributeRegistry.SPELL_POWER.get(), new AttributeModifier(
                    multiplyTotalId, "magic_total", 0.30D, AttributeModifier.Operation.MULTIPLY_TOTAL));
            builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                    vanillaId, "vanilla", 2.0D, AttributeModifier.Operation.ADDITION));

            var merged = MagicAttributeModifierHelper.mergeLinearMagicModifiers(builder.build(), "gametest.merge");
            assertSingleModifierAmount(
                    helper,
                    merged.get(AttributeRegistry.SPELL_POWER.get()),
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    0.30D,
                    "Linear magic modifiers should be combined"
            );
            helper.assertTrue(merged.get(AttributeRegistry.SPELL_POWER.get()).stream()
                            .anyMatch(modifier -> modifier.getId().equals(multiplyTotalId)
                                    && modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL
                                    && Math.abs(modifier.getAmount() - 0.30D) < EPSILON),
                    "MULTIPLY_TOTAL magic modifier should pass through with its UUID");
            helper.assertTrue(merged.get(Attributes.ATTACK_DAMAGE).stream()
                            .anyMatch(modifier -> modifier.getId().equals(vanillaId)
                                    && modifier.getOperation() == AttributeModifier.Operation.ADDITION
                                    && Math.abs(modifier.getAmount() - 2.0D) < EPSILON),
                    "Vanilla combat attribute should pass through with its UUID");
        });
    }

}
