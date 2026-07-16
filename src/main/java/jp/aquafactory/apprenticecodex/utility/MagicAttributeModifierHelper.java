package jp.aquafactory.apprenticecodex.utility;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.attribute.IMagicAttribute;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Iron's の魔法 Attribute modifier を生成・線形合算する共通処理。
 */
public final class MagicAttributeModifierHelper {
    private MagicAttributeModifierHelper() {
    }

    public static void addModifier(
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

    public static Multimap<Attribute, AttributeModifier> mergeLinearMagicModifiers(
            Multimap<Attribute, AttributeModifier> modifiers,
            String modifierSeedPrefix
    ) {
        if (modifiers.isEmpty()) {
            return modifiers;
        }

        var merged = new LinkedHashMap<MergeTarget, Double>();
        var passthrough = new ArrayList<Map.Entry<Attribute, AttributeModifier>>();
        for (var entry : modifiers.entries()) {
            var modifier = entry.getValue();
            var operation = modifier.getOperation();
            if (!(entry.getKey() instanceof IMagicAttribute)
                    || (operation != AttributeModifier.Operation.ADDITION
                    && operation != AttributeModifier.Operation.MULTIPLY_BASE)) {
                passthrough.add(entry);
                continue;
            }

            merged.merge(new MergeTarget(entry.getKey(), operation), modifier.getAmount(), Double::sum);
        }

        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        for (var entry : merged.entrySet()) {
            var target = entry.getKey();
            var amount = entry.getValue();
            if (amount == 0.0D) {
                continue;
            }

            var modifierIdSeed = modifierSeedPrefix
                    + "." + resolveAttributeToken(target.attribute())
                    + "." + target.operation().name().toLowerCase(Locale.ROOT);
            addModifier(builder, target.attribute(), amount, target.operation(), modifierIdSeed);
        }

        passthrough.forEach(builder::put);
        return builder.build();
    }

    private static String resolveAttributeToken(Attribute attribute) {
        var registryKey = ForgeRegistries.ATTRIBUTES.getKey(attribute);
        return registryKey == null ? "unknown" : normalizeKeyToken(registryKey.toString());
    }

    private static String normalizeKeyToken(String token) {
        return Objects.requireNonNull(token)
                .toLowerCase(Locale.ROOT)
                .replace(':', '.')
                .replace('/', '.')
                .replaceAll("[^a-z0-9._-]", "_");
    }

    private record MergeTarget(Attribute attribute, AttributeModifier.Operation operation) {
    }
}
