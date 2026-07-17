package jp.aquafactory.apprenticecodex.item.armor;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import jp.aquafactory.apprenticecodex.utility.MagicAttributeModifierHelper;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

final class MagicArmorAttributeHelper {
    private MagicArmorAttributeHelper() {
    }

    static void addModifier(
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder,
            Attribute attribute,
            double amount,
            AttributeModifier.Operation operation,
            String modifierIdSeed
    ) {
        MagicAttributeModifierHelper.addModifier(builder, attribute, amount, operation, modifierIdSeed);
    }

    static Multimap<Attribute, AttributeModifier> mergeTooltipEquivalentModifiers(
            Multimap<Attribute, AttributeModifier> modifiers,
            String modifierSeedPrefix
    ) {
        return MagicAttributeModifierHelper.mergeLinearMagicModifiers(modifiers, modifierSeedPrefix);
    }
}
