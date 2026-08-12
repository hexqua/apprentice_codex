package jp.aquafactory.apprenticecodex.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class LongStrideMobility extends DynamicCastingMobilityEffect {
    public static final double CASTING_MOVE_SPEED_BONUS = CastingMoveSpeedAdjustment.MAX_CASTING_MOVE_SPEED_BONUS;
    public static final double STEP_HEIGHT_ADDITION = 1.1;
    private static final double MOVE_SPEED_BONUS_BASE = 0.1;
    private static final double MOVE_SPEED_BONUS_PER_LEVEL = 0.05;
    private static final String CASTING_MOVE_SPEED_MODIFIER_ID = "af643a1d-1f2d-4677-bb7e-57dd7af7624d";
    private static final ResourceLocation MOVE_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "long_stride_move_speed");
    private static final ResourceLocation STEP_HEIGHT_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "long_stride_step_height");

    public LongStrideMobility() {
        super(0xAEEB9A, CASTING_MOVE_SPEED_MODIFIER_ID);
        addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                MOVE_SPEED_MODIFIER_ID,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
                amplifier -> MOVE_SPEED_BONUS_BASE + MOVE_SPEED_BONUS_PER_LEVEL * (Math.max(0, amplifier))
        );
        addAttributeModifier(
                Attributes.STEP_HEIGHT,
                STEP_HEIGHT_MODIFIER_ID,
                STEP_HEIGHT_ADDITION,
                AttributeModifier.Operation.ADD_VALUE
        );
    }
}
