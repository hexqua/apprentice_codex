package jp.aquafactory.apprenticecodex.gametest;

import com.google.common.collect.ImmutableMultimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentResolver;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentType;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.MagicAttributeModifierHelper;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

final class AttributeEnchantmentPolicyGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private static final double EPSILON = 1.0e-9D;

    private AttributeEnchantmentPolicyGameTestScenarios() {
    }

    static void forcedEnchantmentsCoexistAndKeepRawPositiveLevels(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = createInitializedPresetStack(ItemRegistry.COPPER_SPELL_AMPLIFIER.get());
            var level = 7;
            var enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            for (var type : AttributeEnchantmentType.values()) {
                var enchantment = enchantments.getOrThrow(type.enchantmentKey());
                stack.enchant(enchantment, level);
            }

            var builder = ImmutableMultimap.<Holder<Attribute>, AttributeModifier>builder();
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
            var attunement = helper.getLevel().registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(AttributeEnchantmentType.ATTUNEMENT.enchantmentKey());
            stack.enchant(attunement, 5);

            var builder = ImmutableMultimap.<Holder<Attribute>, AttributeModifier>builder();
            helper.assertFalse(AttributeEnchantmentResolver.addModifiers(builder, stack, "gametest.attunement_noop"),
                    "Attunement should be a no-op when the imbued school cannot be resolved");
            helper.assertTrue(builder.build().isEmpty(),
                    "Failed Attunement resolution should not add a fallback spell power modifier");
        });
    }

    static void mergeHelperOnlyNormalizesLinearMagicAttributes(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var multiplyTotalId = ResourceLocation.fromNamespaceAndPath("apprenticecodex", "gametest/magic_total");
            var vanillaId = ResourceLocation.fromNamespaceAndPath("apprenticecodex", "gametest/vanilla");
            var builder = ImmutableMultimap.<Holder<Attribute>, AttributeModifier>builder();
            builder.put(AttributeRegistry.SPELL_POWER, new AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath("apprenticecodex", "gametest/magic_a"), 0.10D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            builder.put(AttributeRegistry.SPELL_POWER, new AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath("apprenticecodex", "gametest/magic_b"), 0.20D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            builder.put(AttributeRegistry.SPELL_POWER, new AttributeModifier(
                    multiplyTotalId, 0.30D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                    vanillaId, 2.0D, AttributeModifier.Operation.ADD_VALUE));

            var merged = MagicAttributeModifierHelper.mergeLinearMagicModifiers(builder.build(), "gametest.merge");
            assertSingleModifierAmount(
                    helper,
                    merged.get(AttributeRegistry.SPELL_POWER),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    0.30D,
                    "Linear magic modifiers should be combined"
            );
            helper.assertTrue(merged.get(AttributeRegistry.SPELL_POWER).stream()
                            .anyMatch(modifier -> modifier.id().equals(multiplyTotalId)
                                    && modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                                    && Math.abs(modifier.amount() - 0.30D) < EPSILON),
                    "ADD_MULTIPLIED_TOTAL magic modifier should pass through with its id");
            helper.assertTrue(merged.get(Attributes.ATTACK_DAMAGE).stream()
                            .anyMatch(modifier -> modifier.id().equals(vanillaId)
                                    && modifier.operation() == AttributeModifier.Operation.ADD_VALUE
                                    && Math.abs(modifier.amount() - 2.0D) < EPSILON),
                    "Vanilla combat attribute should pass through with its id");
        });
    }

}
