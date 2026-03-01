package jp.aquafactory.apprenticecodex.effect;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class ThermalProcessing extends MobEffect {
    public static final int BASE_DURATION_TICKS = 40;
    public static final int MAX_AMPLIFIER = 5;
    public static final int IGNITE_TICKS = 240;
    private static final double MOVE_SPEED_DEBUFF_PER_LEVEL = -0.1;
    private static final double ARMOR_DEBUFF_PER_LEVEL = -2.0;
    private static final ResourceLocation MOVE_SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
            "apprenticecodex",
            "thermal_processing_move_speed"
    );
    private static final ResourceLocation ARMOR_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
            "apprenticecodex",
            "thermal_processing_armor"
    );

    public ThermalProcessing() {
        super(MobEffectCategory.HARMFUL, 0xFF8C00);

        addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                MOVE_SPEED_MODIFIER_ID,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
                amplifier -> amplifier <= 0 ? 0.0 : MOVE_SPEED_DEBUFF_PER_LEVEL * amplifier
        );
        addAttributeModifier(
                Attributes.ARMOR,
                ARMOR_MODIFIER_ID,
                AttributeModifier.Operation.ADD_VALUE,
                amplifier -> amplifier <= 1 ? 0.0 : ARMOR_DEBUFF_PER_LEVEL * (amplifier - 1)
        );
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration == 1;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) {
            return true;
        }

        if (amplifier <= 0) {
            return true;
        }

        entity.addEffect(new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(this),
                BASE_DURATION_TICKS,
                amplifier - 1,
                false,
                true,
                true
        ));
        return true;
    }
}
