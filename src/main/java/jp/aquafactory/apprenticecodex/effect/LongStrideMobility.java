package jp.aquafactory.apprenticecodex.effect;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ForgeMod;

import java.util.UUID;

public class LongStrideMobility extends MobEffect {
    public static final double CASTING_MOVE_SPEED_BONUS = 0.85;
    public static final double STEP_HEIGHT_ADDITION = 0.6;
    private static final double MOVE_SPEED_BONUS_PER_LEVEL = 0.15;
    private static final int MAX_AMPLIFIER = 2;
    private static final String CASTING_MOVE_SPEED_MODIFIER_ID = "af643a1d-1f2d-4677-bb7e-57dd7af7624d";
    private static final String MOVE_SPEED_MODIFIER_ID = "26d44a42-e899-41f6-9c6d-7a2ae6f68ef9";
    private static final String STEP_HEIGHT_MODIFIER_ID = "20572320-78a2-4109-bf74-cf73d231d3b4";
    private static final UUID MOVE_SPEED_MODIFIER_UUID = UUID.fromString(MOVE_SPEED_MODIFIER_ID);

    public LongStrideMobility() {
        super(MobEffectCategory.BENEFICIAL, 0xAEEB9A);

        addAttributeModifier(
                AttributeRegistry.CASTING_MOVESPEED.get(),
                CASTING_MOVE_SPEED_MODIFIER_ID,
                CASTING_MOVE_SPEED_BONUS,
                AttributeModifier.Operation.ADDITION
        );
        addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                MOVE_SPEED_MODIFIER_ID,
                MOVE_SPEED_BONUS_PER_LEVEL,
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
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }

    @Override
    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        var clampedAmplifier = Math.max(0, Math.min(MAX_AMPLIFIER, amplifier));
        if (MOVE_SPEED_MODIFIER_UUID.equals(modifier.getId())) {
            return MOVE_SPEED_BONUS_PER_LEVEL * (clampedAmplifier + 1);
        }

        return super.getAttributeModifierValue(amplifier, modifier);
    }
}
