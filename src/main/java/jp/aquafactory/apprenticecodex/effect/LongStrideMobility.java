package jp.aquafactory.apprenticecodex.effect;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class LongStrideMobility extends MobEffect {
    public static final double CASTING_MOVE_SPEED_BONUS = 0.85;
    public static final double STEP_HEIGHT_ADDITION = 0.6;
    private static final double MOVE_SPEED_BONUS_PER_LEVEL = 0.05;
    private static final int MAX_AMPLIFIER = 2;
    private static final ResourceLocation CASTING_MOVE_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "long_stride_casting_move_speed");
    private static final ResourceLocation MOVE_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "long_stride_move_speed");
    private static final ResourceLocation STEP_HEIGHT_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "long_stride_step_height");

    public LongStrideMobility() {
        super(MobEffectCategory.BENEFICIAL, 0xAEEB9A);

        addAttributeModifier(
                AttributeRegistry.CASTING_MOVESPEED,
                CASTING_MOVE_SPEED_MODIFIER_ID,
                CASTING_MOVE_SPEED_BONUS,
                AttributeModifier.Operation.ADD_VALUE
        );
        addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                MOVE_SPEED_MODIFIER_ID,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
                amplifier -> MOVE_SPEED_BONUS_PER_LEVEL * (Math.max(0, Math.min(MAX_AMPLIFIER, amplifier)) + 1)
        );
        addAttributeModifier(
                Attributes.STEP_HEIGHT,
                STEP_HEIGHT_MODIFIER_ID,
                STEP_HEIGHT_ADDITION,
                AttributeModifier.Operation.ADD_VALUE
        );
    }
}
