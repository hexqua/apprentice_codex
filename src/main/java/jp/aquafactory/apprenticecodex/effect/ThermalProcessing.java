package jp.aquafactory.apprenticecodex.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public class ThermalProcessing extends MobEffect {
    public static final int BASE_DURATION_TICKS = 40;
    public static final int MAX_AMPLIFIER = 5;
    public static final int IGNITE_TICKS = 240;
    private static final double MOVE_SPEED_DEBUFF_PER_LEVEL = -0.1;
    private static final double ARMOR_DEBUFF_PER_LEVEL = -2.0;
    private static final String MOVE_SPEED_MODIFIER_ID = "8ccf3692-5f10-4c59-b179-c4b89f048784";
    private static final String ARMOR_MODIFIER_ID = "4d7af06a-4fdf-4028-bff4-08dd5fef9f34";
    private static final UUID MOVE_SPEED_MODIFIER_UUID = UUID.fromString(MOVE_SPEED_MODIFIER_ID);
    private static final UUID ARMOR_MODIFIER_UUID = UUID.fromString(ARMOR_MODIFIER_ID);

    public ThermalProcessing() {
        super(MobEffectCategory.HARMFUL, 0xFF8C00);

        addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                MOVE_SPEED_MODIFIER_ID,
                MOVE_SPEED_DEBUFF_PER_LEVEL,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
        addAttributeModifier(
                Attributes.ARMOR,
                ARMOR_MODIFIER_ID,
                ARMOR_DEBUFF_PER_LEVEL,
                AttributeModifier.Operation.ADDITION
        );
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration == 1;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) {
            return;
        }

        if (amplifier <= 0) {
            return;
        }

        entity.addEffect(new MobEffectInstance(this, BASE_DURATION_TICKS, amplifier - 1, false, true, true));
    }

    @Override
    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        var clampedAmplifier = Math.max(0, Math.min(MAX_AMPLIFIER, amplifier));

        if (MOVE_SPEED_MODIFIER_UUID.equals(modifier.getId())) {
            return clampedAmplifier <= 0 ? 0.0 : MOVE_SPEED_DEBUFF_PER_LEVEL * clampedAmplifier;
        }

        if (ARMOR_MODIFIER_UUID.equals(modifier.getId())) {
            return clampedAmplifier <= 1 ? 0.0 : ARMOR_DEBUFF_PER_LEVEL * (clampedAmplifier - 1);
        }

        return super.getAttributeModifierValue(amplifier, modifier);
    }
}
