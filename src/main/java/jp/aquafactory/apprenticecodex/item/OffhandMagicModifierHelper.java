package jp.aquafactory.apprenticecodex.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

public final class OffhandMagicModifierHelper {
    private static final double ALACRITY_COOLDOWN_REDUCTION_PER_LEVEL = 0.02D;
    private static final double REFLUX_MANA_REGEN_PER_LEVEL = 0.05D;
    private static final double RESERVOIR_MAX_MANA_PER_LEVEL = 20.0D;
    private static final double SURGE_SPELL_POWER_PER_LEVEL = 0.02D;
    private static final double ATTUNEMENT_SPELL_POWER_PER_LEVEL = 0.04D;
    private static final double TENSE_CAST_TIME_REDUCTION_PER_LEVEL = 0.05D;

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
        if (stack == null || stack.isEmpty()) {
            return baseModifiers;
        }

        var builder = ImmutableMultimap.<Holder<Attribute>, AttributeModifier>builder();
        builder.putAll(baseModifiers);

        var alacrityLevel = Enchantments.getLevel(stack, Enchantments.ALACRITY);
        var refluxLevel = Enchantments.getLevel(stack, Enchantments.REFLUX);
        var reservoirLevel = Enchantments.getLevel(stack, Enchantments.RESERVOIR);
        var surgeLevel = Enchantments.getLevel(stack, Enchantments.SURGE);
        var attunementLevel = Enchantments.getLevel(stack, Enchantments.ATTUNEMENT);
        var tenseLevel = Enchantments.getLevel(stack, Enchantments.TENSE);

        addEquippedModifier(
                builder,
                AttributeRegistry.COOLDOWN_REDUCTION,
                alacrityLevel * ALACRITY_COOLDOWN_REDUCTION_PER_LEVEL,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                createModifierId(itemKey, "alacrity_cooldown_reduction")
        );
        addEquippedModifier(
                builder,
                AttributeRegistry.MANA_REGEN,
                refluxLevel * REFLUX_MANA_REGEN_PER_LEVEL,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                createModifierId(itemKey, "reflux_mana_regen")
        );
        addEquippedModifier(
                builder,
                AttributeRegistry.MAX_MANA,
                reservoirLevel * RESERVOIR_MAX_MANA_PER_LEVEL,
                AttributeModifier.Operation.ADD_VALUE,
                createModifierId(itemKey, "reservoir_max_mana")
        );
        addEquippedModifier(
                builder,
                AttributeRegistry.SPELL_POWER,
                surgeLevel * SURGE_SPELL_POWER_PER_LEVEL,
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
                        attunementLevel * ATTUNEMENT_SPELL_POWER_PER_LEVEL,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                        createModifierId(itemKey, "attunement_spell_power")
                );
            }
        }

        addEquippedModifier(
                builder,
                AttributeRegistry.CAST_TIME_REDUCTION,
                tenseLevel * TENSE_CAST_TIME_REDUCTION_PER_LEVEL,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                createModifierId(itemKey, "tense_cast_time_reduction")
        );
        return builder.build();
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
