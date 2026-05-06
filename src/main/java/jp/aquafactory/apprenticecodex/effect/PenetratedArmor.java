package jp.aquafactory.apprenticecodex.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public class PenetratedArmor extends MobEffect {
    private static final double ARMOR_REDUCTION_PER_LEVEL = -0.2D;
    private static final double ARMOR_TOUGHNESS_REDUCTION = -1.0D;
    private static final int MAX_AMPLIFIER = 3;
    private static final String ARMOR_MODIFIER_ID = "bb61a53b-815f-4d1b-8d38-3836d7df380f";
    private static final String ARMOR_TOUGHNESS_MODIFIER_ID = "2c90a296-8b02-4fef-9ab1-0a7f09a1ec63";
    private static final UUID ARMOR_MODIFIER_UUID = UUID.fromString(ARMOR_MODIFIER_ID);
    private static final UUID ARMOR_TOUGHNESS_MODIFIER_UUID = UUID.fromString(ARMOR_TOUGHNESS_MODIFIER_ID);

    public PenetratedArmor() {
        super(MobEffectCategory.HARMFUL, 0xAEEB9A);

        addAttributeModifier(
                Attributes.ARMOR,
                ARMOR_MODIFIER_ID,
                ARMOR_REDUCTION_PER_LEVEL,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
        addAttributeModifier(
                Attributes.ARMOR_TOUGHNESS,
                ARMOR_TOUGHNESS_MODIFIER_ID,
                ARMOR_TOUGHNESS_REDUCTION,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }

    @Override
    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        var clampedAmplifier = Math.max(0, Math.min(MAX_AMPLIFIER, amplifier));

        if (ARMOR_MODIFIER_UUID.equals(modifier.getId())) {
            return ARMOR_REDUCTION_PER_LEVEL * (clampedAmplifier + 1);
        }

        if (ARMOR_TOUGHNESS_MODIFIER_UUID.equals(modifier.getId())) {
            return ARMOR_TOUGHNESS_REDUCTION;
        }

        return super.getAttributeModifierValue(amplifier, modifier);
    }
}
