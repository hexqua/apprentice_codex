package jp.aquafactory.apprenticecodex.enchantment;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import jp.aquafactory.apprenticecodex.utility.MagicAttributeModifierHelper;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * Stack に存在する6種のエンチャントを、装備場所に依存しない modifier へ解決する。
 */
public final class AttributeEnchantmentResolver {
    private static final AdditionalModifierAppender NO_ADDITIONAL_MODIFIERS =
            (builder, stack, modifierSeedPrefix) -> false;

    private AttributeEnchantmentResolver() {
    }

    public static Multimap<Attribute, AttributeModifier> resolveMergedModifiers(
            Multimap<Attribute, AttributeModifier> baseModifiers,
            ItemStack stack,
            String modifierSeedPrefix
    ) {
        return resolveMergedModifiers(
                baseModifiers,
                stack,
                modifierSeedPrefix,
                NO_ADDITIONAL_MODIFIERS
        );
    }

    public static Multimap<Attribute, AttributeModifier> resolveMergedModifiers(
            Multimap<Attribute, AttributeModifier> baseModifiers,
            ItemStack stack,
            String modifierSeedPrefix,
            AdditionalModifierAppender additionalModifierAppender
    ) {
        return resolveMergedModifiers(
                baseModifiers,
                stack,
                modifierSeedPrefix,
                AttributeEnchantmentPolicy.ALL_ATTRIBUTE_ENCHANTMENTS,
                additionalModifierAppender
        );
    }

    public static Multimap<Attribute, AttributeModifier> resolveMergedModifiers(
            Multimap<Attribute, AttributeModifier> baseModifiers,
            ItemStack stack,
            String modifierSeedPrefix,
            Set<AttributeEnchantmentType> effectiveEnchantments
    ) {
        return resolveMergedModifiers(
                baseModifiers,
                stack,
                modifierSeedPrefix,
                effectiveEnchantments,
                NO_ADDITIONAL_MODIFIERS
        );
    }

    private static Multimap<Attribute, AttributeModifier> resolveMergedModifiers(
            Multimap<Attribute, AttributeModifier> baseModifiers,
            ItemStack stack,
            String modifierSeedPrefix,
            Set<AttributeEnchantmentType> effectiveEnchantments,
            AdditionalModifierAppender additionalModifierAppender
    ) {
        if (stack == null || stack.isEmpty()) {
            return baseModifiers;
        }

        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        builder.putAll(baseModifiers);
        var hasAdditionalModifiers =
                additionalModifierAppender.add(builder, stack, modifierSeedPrefix + ".stack");
        var hasEnchantmentModifiers = addModifiers(
                builder,
                stack,
                modifierSeedPrefix + ".enchant",
                effectiveEnchantments
        );
        if (!hasAdditionalModifiers && !hasEnchantmentModifiers) {
            return baseModifiers;
        }

        return MagicAttributeModifierHelper.mergeLinearMagicModifiers(
                builder.build(),
                modifierSeedPrefix + ".merged"
        );
    }

    public static boolean addModifiers(
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder,
            ItemStack stack,
            String modifierSeedPrefix
    ) {
        return addModifiers(
                builder,
                stack,
                modifierSeedPrefix,
                AttributeEnchantmentPolicy.ALL_ATTRIBUTE_ENCHANTMENTS
        );
    }

    public static boolean addModifiers(
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder,
            ItemStack stack,
            String modifierSeedPrefix,
            Set<AttributeEnchantmentType> effectiveEnchantments
    ) {
        if (stack == null || stack.isEmpty()
                || !(stack.getItem() instanceof AttributeEnchantmentPolicy)) {
            return false;
        }

        var added = false;
        for (var type : AttributeEnchantmentType.values()) {
            if (!effectiveEnchantments.contains(type)) {
                continue;
            }
            var level = type.getLevel(stack);
            if (level <= 0) {
                continue;
            }

            var attribute = type.resolveAttribute(stack);
            if (attribute == null) {
                continue;
            }

            MagicAttributeModifierHelper.addModifier(
                    builder,
                    attribute,
                    level * type.amountPerLevel(),
                    type.operation(),
                    modifierSeedPrefix + "." + type.modifierKey()
            );
            added = true;
        }
        return added;
    }

    @FunctionalInterface
    public interface AdditionalModifierAppender {
        boolean add(
                ImmutableMultimap.Builder<Attribute, AttributeModifier> builder,
                ItemStack stack,
                String modifierSeedPrefix
        );
    }
}
