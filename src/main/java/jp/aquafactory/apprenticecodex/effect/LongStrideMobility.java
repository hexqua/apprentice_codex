package jp.aquafactory.apprenticecodex.effect;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ForgeMod;

import java.util.UUID;

public class LongStrideMobility extends DynamicCastingMobilityEffect {
    public static final double CASTING_MOVE_SPEED_BONUS = CastingMoveSpeedAdjustment.MAX_CASTING_MOVE_SPEED_BONUS;
    public static final double STEP_HEIGHT_ADDITION = 1.1;
    private static final double MOVE_SPEED_BONUS_BASE = 0.1;
    private static final double MOVE_SPEED_BONUS_PER_LEVEL = 0.05;
    private static final String CASTING_MOVE_SPEED_MODIFIER_ID = "af643a1d-1f2d-4677-bb7e-57dd7af7624d";
    private static final String MOVE_SPEED_MODIFIER_ID = "26d44a42-e899-41f6-9c6d-7a2ae6f68ef9";
    private static final String STEP_HEIGHT_MODIFIER_ID = "20572320-78a2-4109-bf74-cf73d231d3b4";
    private static final UUID MOVE_SPEED_MODIFIER_UUID = UUID.fromString(MOVE_SPEED_MODIFIER_ID);

    public LongStrideMobility() {
        super(0xAEEB9A, CASTING_MOVE_SPEED_MODIFIER_ID);
        addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                MOVE_SPEED_MODIFIER_ID,
                MOVE_SPEED_BONUS_BASE,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
        addAttributeModifier(
                ForgeMod.STEP_HEIGHT_ADDITION.get(),
                STEP_HEIGHT_MODIFIER_ID,
                STEP_HEIGHT_ADDITION,
                AttributeModifier.Operation.ADDITION
        );
    }

    @Override
    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        if (MOVE_SPEED_MODIFIER_UUID.equals(modifier.getId())) {
            return MOVE_SPEED_BONUS_BASE + MOVE_SPEED_BONUS_PER_LEVEL * Math.max(0, amplifier);
        }

        return super.getAttributeModifierValue(amplifier, modifier);
    }
}
