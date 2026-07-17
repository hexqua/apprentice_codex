package jp.aquafactory.apprenticecodex.utility;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.attribute.IMagicAttribute;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Iron's の魔法 Attribute modifier を生成・線形合算する共通処理。
 */
public final class MagicAttributeModifierHelper {
    private MagicAttributeModifierHelper() {
    }

    public static void addModifier(
            ImmutableMultimap.Builder<Holder<Attribute>, AttributeModifier> builder,
            Holder<Attribute> attribute,
            double amount,
            AttributeModifier.Operation operation,
            String modifierIdSeed
    ) {
        if (attribute == null || amount == 0.0D) {
            return;
        }

        builder.put(attribute, new AttributeModifier(createModifierId(modifierIdSeed), amount, operation));
    }

    public static Multimap<Holder<Attribute>, AttributeModifier> mergeLinearMagicModifiers(
            Multimap<Holder<Attribute>, AttributeModifier> modifiers,
            String modifierSeedPrefix
    ) {
        if (modifiers.isEmpty()) {
            return modifiers;
        }

        var merged = new LinkedHashMap<MergeTarget, Double>();
        var passthrough = new ArrayList<Map.Entry<Holder<Attribute>, AttributeModifier>>();
        for (var entry : modifiers.entries()) {
            var modifier = entry.getValue();
            var operation = modifier.operation();
            if (!(entry.getKey().value() instanceof IMagicAttribute)
                    || (operation != AttributeModifier.Operation.ADD_VALUE
                    && operation != AttributeModifier.Operation.ADD_MULTIPLIED_BASE)) {
                passthrough.add(entry);
                continue;
            }

            merged.merge(new MergeTarget(entry.getKey(), operation), modifier.amount(), Double::sum);
        }

        var builder = ImmutableMultimap.<Holder<Attribute>, AttributeModifier>builder();
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

    private static String resolveAttributeToken(Holder<Attribute> attribute) {
        var registryKey = attribute.unwrapKey().map(ResourceKey::location).orElseGet(() ->
                BuiltInRegistries.ATTRIBUTE.getKey(attribute.value()));
        return registryKey == null ? "unknown" : normalizeKeyToken(registryKey.toString());
    }

    public static ResourceLocation createModifierId(String seed) {
        return ResourceLocation.fromNamespaceAndPath(
                ApprenticeCodex.MODID,
                normalizeKeyToken(seed).replace('.', '/')
        );
    }

    private static String normalizeKeyToken(String token) {
        return Objects.requireNonNull(token)
                .toLowerCase(Locale.ROOT)
                .replace(':', '.')
                .replace('/', '.')
                .replaceAll("[^a-z0-9._-]", "_");
    }

    private record MergeTarget(Holder<Attribute> attribute, AttributeModifier.Operation operation) {
    }
}
