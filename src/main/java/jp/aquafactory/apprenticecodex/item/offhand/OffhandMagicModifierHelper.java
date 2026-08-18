package jp.aquafactory.apprenticecodex.item.offhand;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentPolicy;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentType;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

public final class OffhandMagicModifierHelper {
    private OffhandMagicModifierHelper() {
    }

    public static int enchantmentValue() {
        return 1;
    }

    public static boolean isEnchantable(ItemStack stack) {
        return enchantmentValue() > 0;
    }

    public static Multimap<Holder<Attribute>, AttributeModifier> buildEquippedModifiers(
            Multimap<Holder<Attribute>, AttributeModifier> baseModifiers,
            ItemStack stack,
            String itemKey
    ) {
        return buildEquippedModifiers(
                baseModifiers,
                stack,
                itemKey,
                AttributeEnchantmentPolicy.ALL_ATTRIBUTE_ENCHANTMENTS
        );
    }

    public static Multimap<Holder<Attribute>, AttributeModifier> buildEquippedModifiers(
            Multimap<Holder<Attribute>, AttributeModifier> baseModifiers,
            ItemStack stack,
            String itemKey,
            Set<AttributeEnchantmentType> effectiveEnchantments
    ) {
        if (stack == null || stack.isEmpty()) {
            return baseModifiers;
        }

        var builder = ImmutableMultimap.<Holder<Attribute>, AttributeModifier>builder();
        builder.putAll(baseModifiers);

        var alacrityLevel = getEffectiveLevel(stack, effectiveEnchantments, AttributeEnchantmentType.ALACRITY);
        var refluxLevel = getEffectiveLevel(stack, effectiveEnchantments, AttributeEnchantmentType.REFLUX);
        var reservoirLevel = getEffectiveLevel(stack, effectiveEnchantments, AttributeEnchantmentType.RESERVOIR);
        var surgeLevel = getEffectiveLevel(stack, effectiveEnchantments, AttributeEnchantmentType.SURGE);
        var attunementLevel = getEffectiveLevel(stack, effectiveEnchantments, AttributeEnchantmentType.ATTUNEMENT);
        var tenseLevel = getEffectiveLevel(stack, effectiveEnchantments, AttributeEnchantmentType.TENSE);

        addEquippedModifier(
                builder,
                AttributeRegistry.COOLDOWN_REDUCTION,
                alacrityLevel * AttributeEnchantmentType.ALACRITY.amountPerLevel(),
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                createModifierId(itemKey, "alacrity_cooldown_reduction")
        );
        addEquippedModifier(
                builder,
                AttributeRegistry.MANA_REGEN,
                refluxLevel * AttributeEnchantmentType.REFLUX.amountPerLevel(),
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                createModifierId(itemKey, "reflux_mana_regen")
        );
        addEquippedModifier(
                builder,
                AttributeRegistry.MAX_MANA,
                reservoirLevel * AttributeEnchantmentType.RESERVOIR.amountPerLevel(),
                AttributeModifier.Operation.ADD_VALUE,
                createModifierId(itemKey, "reservoir_max_mana")
        );
        addEquippedModifier(
                builder,
                AttributeRegistry.SPELL_POWER,
                surgeLevel * AttributeEnchantmentType.SURGE.amountPerLevel(),
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                createModifierId(itemKey, "surge_spell_power")
        );

        if (attunementLevel > 0) {
            var imbuedSchool = MagicTools.getImbuedSpellSchool(stack);
            var attunementSpellPowerAttribute = MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
            if (attunementSpellPowerAttribute != null) {
                addEquippedModifier(
                        builder,
                        BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attunementSpellPowerAttribute),
                        attunementLevel * AttributeEnchantmentType.ATTUNEMENT.amountPerLevel(),
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                        createModifierId(itemKey, "attunement_spell_power")
                );
            }
        }

        addEquippedModifier(
                builder,
                AttributeRegistry.CAST_TIME_REDUCTION,
                tenseLevel * AttributeEnchantmentType.TENSE.amountPerLevel(),
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                createModifierId(itemKey, "tense_cast_time_reduction")
        );
        return builder.build();
    }

    private static int getEffectiveLevel(
            ItemStack stack,
            Set<AttributeEnchantmentType> effectiveEnchantments,
            AttributeEnchantmentType type
    ) {
        return effectiveEnchantments.contains(type) ? Enchantments.getLevel(stack, type.enchantmentKey()) : 0;
    }

    public static void addEquippedModifier(
            ImmutableMultimap.Builder<Holder<Attribute>, AttributeModifier> builder,
            Holder<Attribute> attribute,
            double amount,
            AttributeModifier.Operation operation,
            ResourceLocation modifierId
    ) {
        if (attribute == null || amount == 0.0D) {
            return;
        }

        builder.put(attribute, new AttributeModifier(modifierId, amount, operation));
    }

    private static ResourceLocation createModifierId(String itemKey, String modifierKey) {
        return ResourceLocation.fromNamespaceAndPath(
                ApprenticeCodex.MODID,
                normalizeKeyToken(itemKey) + "/" + modifierKey
        );
    }

    private static String normalizeKeyToken(String token) {
        return token.toLowerCase(java.util.Locale.ROOT)
                .replace(':', '_')
                .replace('/', '_')
                .replace('.', '_')
                .replaceAll("[^a-z0-9_-]", "_");
    }
}
