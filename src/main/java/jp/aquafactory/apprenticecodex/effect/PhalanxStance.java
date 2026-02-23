package jp.aquafactory.apprenticecodex.effect;

import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class PhalanxStance extends MobEffect {
    private static final float MIN_MOVE_SPEED_MULTIPLIER = 0.40f;
    private static final float MAX_MOVE_SPEED_MULTIPLIER = 1.00f;
    private static final float MOVE_SPEED_STEP = 0.05f;
    private static final int MAX_AMPLIFIER = Math.round((MAX_MOVE_SPEED_MULTIPLIER - MIN_MOVE_SPEED_MULTIPLIER) / MOVE_SPEED_STEP);

    public PhalanxStance() {
        super(MobEffectCategory.BENEFICIAL, 0x6CA9FF);
        addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                "9f449650-b236-4ebc-99f2-8a1e8e52bb4b",
                toMoveSpeedMultiplier(0) - 1.0,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
    }

    public static int toAmplifier(float moveSpeedMultiplier) {
        var clamped = Mth.clamp(moveSpeedMultiplier, MIN_MOVE_SPEED_MULTIPLIER, MAX_MOVE_SPEED_MULTIPLIER);
        var scaled = Math.round((clamped - MIN_MOVE_SPEED_MULTIPLIER) / MOVE_SPEED_STEP);
        return Mth.clamp(scaled, 0, MAX_AMPLIFIER);
    }

    private static float toMoveSpeedMultiplier(int amplifier) {
        return Mth.clamp(
                MIN_MOVE_SPEED_MULTIPLIER + MOVE_SPEED_STEP * amplifier,
                MIN_MOVE_SPEED_MULTIPLIER,
                MAX_MOVE_SPEED_MULTIPLIER
        );
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }

    @Override
    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        return toMoveSpeedMultiplier(amplifier) - 1.0;
    }
}
