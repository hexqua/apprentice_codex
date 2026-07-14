package jp.aquafactory.apprenticecodex.item.offhand;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class OffhandMagicModifierHelper {
    private static final double ALACRITY_COOLDOWN_REDUCTION_PER_LEVEL = 0.02D;
    private static final double REFLUX_MANA_REGEN_PER_LEVEL = 0.05D;
    private static final double RESERVOIR_MAX_MANA_PER_LEVEL = 20.0D;
    private static final double SURGE_SPELL_POWER_PER_LEVEL = 0.02D;
    private static final double ATTUNEMENT_SPELL_POWER_PER_LEVEL = 0.04D;
    private static final double TENSE_CAST_TIME_REDUCTION_PER_LEVEL = 0.05D;
    private static final StackDependentModifierAppender NO_OP_APPENDER =
            (builder, stack, modifierSeedPrefix) -> false;

    private OffhandMagicModifierHelper() {
    }

    public static int enchantmentValue() {
        return 1;
    }

    public static boolean isEnchantable(ItemStack stack) {
        return enchantmentValue() > 0;
    }

    public static Multimap<Attribute, AttributeModifier> buildEquippedModifiers(
            Multimap<Attribute, AttributeModifier> baseModifiers,
            ItemStack stack,
            String itemKey
    ) {
        return buildEquippedModifiers(baseModifiers, stack, itemKey, NO_OP_APPENDER);
    }

    public static Multimap<Attribute, AttributeModifier> buildEquippedModifiers(
            Multimap<Attribute, AttributeModifier> baseModifiers,
            ItemStack stack,
            String itemKey,
            StackDependentModifierAppender stackDependentAppender
    ) {
        if (stack == null || stack.isEmpty()) {
            return baseModifiers;
        }

        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        builder.putAll(baseModifiers);
        var prefix = "apprenticecodex." + normalizeKeyToken(itemKey);
        var hasStackDependentModifiers = stackDependentAppender.add(builder, stack, prefix + ".stack");

        if (!stack.isEnchanted()) {
            return hasStackDependentModifiers
                    ? mergeTooltipEquivalentModifiers(builder.build(), prefix + ".merged")
                    : baseModifiers;
        }
        if (!EnchantmentRegistry.ALACRITY.isPresent()
                || !EnchantmentRegistry.REFLUX.isPresent()
                || !EnchantmentRegistry.RESERVOIR.isPresent()
                || !EnchantmentRegistry.SURGE.isPresent()
                || !EnchantmentRegistry.ATTUNEMENT.isPresent()
                || !EnchantmentRegistry.TENSE.isPresent()) {
            return hasStackDependentModifiers
                    ? mergeTooltipEquivalentModifiers(builder.build(), prefix + ".merged")
                    : baseModifiers;
        }

        var alacrityLevel = stack.getEnchantmentLevel(EnchantmentRegistry.ALACRITY.get());
        var refluxLevel = stack.getEnchantmentLevel(EnchantmentRegistry.REFLUX.get());
        var reservoirLevel = stack.getEnchantmentLevel(EnchantmentRegistry.RESERVOIR.get());
        var surgeLevel = stack.getEnchantmentLevel(EnchantmentRegistry.SURGE.get());
        var attunementLevel = stack.getEnchantmentLevel(EnchantmentRegistry.ATTUNEMENT.get());
        var tenseLevel = stack.getEnchantmentLevel(EnchantmentRegistry.TENSE.get());

        if (alacrityLevel <= 0
                && refluxLevel <= 0
                && reservoirLevel <= 0
                && surgeLevel <= 0
                && attunementLevel <= 0
                && tenseLevel <= 0) {
            return hasStackDependentModifiers
                    ? mergeTooltipEquivalentModifiers(builder.build(), prefix + ".merged")
                    : baseModifiers;
        }

        var enchantPrefix = prefix + ".enchant";

        addEquippedModifier(
                builder,
                AttributeRegistry.COOLDOWN_REDUCTION.get(),
                alacrityLevel * ALACRITY_COOLDOWN_REDUCTION_PER_LEVEL,
                AttributeModifier.Operation.MULTIPLY_BASE,
                enchantPrefix + ".alacrity.cooldown_reduction"
        );
        addEquippedModifier(
                builder,
                AttributeRegistry.MANA_REGEN.get(),
                refluxLevel * REFLUX_MANA_REGEN_PER_LEVEL,
                AttributeModifier.Operation.MULTIPLY_BASE,
                enchantPrefix + ".reflux.mana_regen"
        );
        addEquippedModifier(
                builder,
                AttributeRegistry.MAX_MANA.get(),
                reservoirLevel * RESERVOIR_MAX_MANA_PER_LEVEL,
                AttributeModifier.Operation.ADDITION,
                enchantPrefix + ".reservoir.max_mana"
        );
        addEquippedModifier(
                builder,
                AttributeRegistry.SPELL_POWER.get(),
                surgeLevel * SURGE_SPELL_POWER_PER_LEVEL,
                AttributeModifier.Operation.MULTIPLY_BASE,
                enchantPrefix + ".surge.spell_power"
        );
        if (attunementLevel > 0) {
            var imbuedSchool = MagicTools.getImbuedSpellSchool(stack);
            var attunementSpellPowerAttribute = MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
            addEquippedModifier(
                    builder,
                    attunementSpellPowerAttribute,
                    attunementLevel * ATTUNEMENT_SPELL_POWER_PER_LEVEL,
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    enchantPrefix + ".attunement.spell_power"
            );
        }
        addEquippedModifier(
                builder,
                AttributeRegistry.CAST_TIME_REDUCTION.get(),
                tenseLevel * TENSE_CAST_TIME_REDUCTION_PER_LEVEL,
                AttributeModifier.Operation.MULTIPLY_BASE,
                enchantPrefix + ".tense.cast_time_reduction"
        );

        return mergeTooltipEquivalentModifiers(builder.build(), prefix + ".merged");
    }

    public static void addEquippedModifier(
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder,
            Attribute attribute,
            double amount,
            AttributeModifier.Operation operation,
            String modifierIdSeed
    ) {
        if (attribute == null || amount == 0.0D) {
            return;
        }

        var modifierId = UUID.nameUUIDFromBytes(modifierIdSeed.getBytes(StandardCharsets.UTF_8));
        builder.put(attribute, new AttributeModifier(modifierId, modifierIdSeed, amount, operation));
    }

    private static Multimap<Attribute, AttributeModifier> mergeTooltipEquivalentModifiers(
            Multimap<Attribute, AttributeModifier> modifiers,
            String modifierSeedPrefix
    ) {
        if (modifiers.isEmpty()) {
            return modifiers;
        }

        var merged = new LinkedHashMap<MergeTarget, Double>();
        var passthrough = new java.util.ArrayList<Map.Entry<Attribute, AttributeModifier>>();
        for (var entry : modifiers.entries()) {
            var modifier = entry.getValue();
            var operation = modifier.getOperation();
            // MULTIPLY_TOTAL は線形合算できないため、挙動維持のためそのまま残す.
            if (!isMergeableMagicAttribute(entry.getKey())
                    || (operation != AttributeModifier.Operation.ADDITION
                    && operation != AttributeModifier.Operation.MULTIPLY_BASE)) {
                passthrough.add(entry);
                continue;
            }

            var key = new MergeTarget(entry.getKey(), operation);
            merged.merge(key, modifier.getAmount(), Double::sum);
        }

        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        for (var entry : merged.entrySet()) {
            var target = entry.getKey();
            var amount = entry.getValue();
            if (amount == 0.0D) {
                continue;
            }

            var operationToken = target.operation().name().toLowerCase(Locale.ROOT);
            var attributeToken = resolveAttributeToken(target.attribute());
            var modifierIdSeed = modifierSeedPrefix + "." + attributeToken + "." + operationToken;
            var modifierId = UUID.nameUUIDFromBytes(modifierIdSeed.getBytes(StandardCharsets.UTF_8));
            builder.put(
                    target.attribute(),
                    new AttributeModifier(modifierId, modifierIdSeed, amount, target.operation())
            );
        }

        for (var entry : passthrough) {
            builder.put(entry);
        }
        return builder.build();
    }

    private static boolean isMergeableMagicAttribute(Attribute attribute) {
        return attribute == AttributeRegistry.COOLDOWN_REDUCTION.get()
                || attribute == AttributeRegistry.MANA_REGEN.get()
                || attribute == AttributeRegistry.MAX_MANA.get()
                || attribute == AttributeRegistry.SPELL_POWER.get()
                || attribute == AttributeRegistry.CAST_TIME_REDUCTION.get()
                || attribute == AttributeRegistry.CASTING_MOVESPEED.get()
                || attribute == AttributeRegistry.FIRE_SPELL_POWER.get()
                || attribute == AttributeRegistry.ICE_SPELL_POWER.get()
                || attribute == AttributeRegistry.LIGHTNING_SPELL_POWER.get()
                || attribute == AttributeRegistry.HOLY_SPELL_POWER.get()
                || attribute == AttributeRegistry.ENDER_SPELL_POWER.get()
                || attribute == AttributeRegistry.BLOOD_SPELL_POWER.get()
                || attribute == AttributeRegistry.EVOCATION_SPELL_POWER.get()
                || attribute == AttributeRegistry.NATURE_SPELL_POWER.get();
    }

    private static String resolveAttributeToken(Attribute attribute) {
        var registryKey = ForgeRegistries.ATTRIBUTES.getKey(attribute);
        if (registryKey == null) {
            return "unknown";
        }
        return normalizeKeyToken(registryKey.toString());
    }

    private static String normalizeKeyToken(String token) {
        return token.toLowerCase(Locale.ROOT)
                .replace(':', '.')
                .replace('/', '.')
                .replaceAll("[^a-z0-9._-]", "_");
    }

    @FunctionalInterface
    public interface StackDependentModifierAppender {
        boolean add(
                ImmutableMultimap.Builder<Attribute, AttributeModifier> builder,
                ItemStack stack,
                String modifierSeedPrefix
        );
    }

    private record MergeTarget(
            Attribute attribute,
            AttributeModifier.Operation operation
    ) {
    }
}
