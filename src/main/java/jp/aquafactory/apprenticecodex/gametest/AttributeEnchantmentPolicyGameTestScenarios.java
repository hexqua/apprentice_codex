package jp.aquafactory.apprenticecodex.gametest;

import com.google.common.collect.ImmutableMultimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentPolicy;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentResolver;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentType;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.MagicAttributeModifierHelper;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Set;
import java.util.UUID;

final class AttributeEnchantmentPolicyGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private static final double EPSILON = 1.0e-9D;

    private AttributeEnchantmentPolicyGameTestScenarios() {
    }

    static void directApplicationPolicyKeepsExpectedMatrix(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var all = AttributeEnchantmentPolicy.ALL_ATTRIBUTE_ENCHANTMENTS;
            var cases = List.of(
                    new PolicyCase(ItemRegistry.COPPER_SPELL_AMPLIFIER.get(), all),
                    new PolicyCase(ItemRegistry.ENCHANTED_CIRCLET.get(), all),
                    new PolicyCase(ItemRegistry.IRON_SPELLCASTER_GUN.get(), all),
                    new PolicyCase(ItemRegistry.SCROLLCASTER_GAUNTLET.get(), Set.of()),
                    new PolicyCase(ItemRegistry.MANA_FORCE_BLADE.get(),
                            Set.of(AttributeEnchantmentType.SURGE, AttributeEnchantmentType.ATTUNEMENT)),
                    new PolicyCase(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get(),
                            Set.of(
                                    AttributeEnchantmentType.ALACRITY,
                                    AttributeEnchantmentType.REFLUX,
                                    AttributeEnchantmentType.RESERVOIR,
                                    AttributeEnchantmentType.SURGE,
                                    AttributeEnchantmentType.TENSE
                            )),
                    new PolicyCase(ItemRegistry.PARRYCAST_BUCKLER.get(),
                            Set.of(AttributeEnchantmentType.ALACRITY, AttributeEnchantmentType.TENSE)),
                    new PolicyCase(ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get(),
                            Set.of(AttributeEnchantmentType.SURGE, AttributeEnchantmentType.ATTUNEMENT)),
                    new PolicyCase(ItemRegistry.ELEMENT_MAIDEN_ROBE_RIBBON.get(), Set.of()),
                    new PolicyCase(ItemRegistry.ELEMENT_MAIDEN_ROBE_LEGGINGS.get(), Set.of()),
                    new PolicyCase(ItemRegistry.ELEMENT_MAIDEN_ROBE_BOOTS.get(), Set.of())
            );

            for (var testCase : cases) {
                helper.assertTrue(testCase.item() instanceof AttributeEnchantmentPolicy,
                        testCase.item().getDescriptionId() + " should participate in the attribute enchantment policy");
                var policy = (AttributeEnchantmentPolicy) testCase.item();
                helper.assertTrue(policy.directlyApplicableAttributeEnchantments().equals(testCase.directlyApplicable()),
                        testCase.item().getDescriptionId() + " direct attribute enchantment policy changed");

                for (var type : AttributeEnchantmentType.values()) {
                    var enchantment = ForgeRegistries.ENCHANTMENTS.getValue(type.enchantmentId());
                    helper.assertTrue(enchantment != null,
                            "Attribute enchantment was not registered: " + type.enchantmentId());
                    helper.assertTrue(
                            enchantment.canEnchant(new ItemStack(testCase.item()))
                                    == testCase.directlyApplicable().contains(type),
                            testCase.item().getDescriptionId() + " category result changed for " + type
                    );
                }
            }
        });
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

    private record PolicyCase(Item item, Set<AttributeEnchantmentType> directlyApplicable) {
    }
}
