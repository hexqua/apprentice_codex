package jp.aquafactory.apprenticecodex.effect;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.effect.MagicMobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ForgeMod;

import java.util.UUID;

public class MistFormEffect extends MagicMobEffect {
    public static final double MOVEMENT_SPEED_BONUS = 0.20D;
    public static final double STEP_HEIGHT_ADDITION = 0.5D;
    public static final double SCHOOL_RESIST_WEAKNESS = -1.0D;
    public static final float JUMP_POWER_ADDITION = 0.3F;

    private static final String MOVEMENT_SPEED_MODIFIER_ID = "45a2d80d-0ec7-4ab8-8238-7c095ea85fef";
    private static final String STEP_HEIGHT_MODIFIER_ID = "e950f6c2-8762-4a12-b02e-feb0fc911433";
    private static final String FIRE_RESIST_MODIFIER_ID = "22960816-46dc-4ead-aef2-b30f8efaf129";
    private static final String HOLY_RESIST_MODIFIER_ID = "f825bba6-fd15-4479-a1e2-b6130e072a1b";
    private static final UUID MOVEMENT_SPEED_MODIFIER_UUID = UUID.fromString(MOVEMENT_SPEED_MODIFIER_ID);
    private static final UUID STEP_HEIGHT_MODIFIER_UUID = UUID.fromString(STEP_HEIGHT_MODIFIER_ID);
    private static final UUID FIRE_RESIST_MODIFIER_UUID = UUID.fromString(FIRE_RESIST_MODIFIER_ID);
    private static final UUID HOLY_RESIST_MODIFIER_UUID = UUID.fromString(HOLY_RESIST_MODIFIER_ID);

    public MistFormEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xF4FCFF);
        addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                MOVEMENT_SPEED_MODIFIER_ID,
                MOVEMENT_SPEED_BONUS,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
        addAttributeModifier(
                ForgeMod.STEP_HEIGHT_ADDITION.get(),
                STEP_HEIGHT_MODIFIER_ID,
                STEP_HEIGHT_ADDITION,
                AttributeModifier.Operation.ADDITION
        );
        addAttributeModifier(
                AttributeRegistry.FIRE_MAGIC_RESIST.get(),
                FIRE_RESIST_MODIFIER_ID,
                SCHOOL_RESIST_WEAKNESS,
                AttributeModifier.Operation.ADDITION
        );
        addAttributeModifier(
                AttributeRegistry.HOLY_MAGIC_RESIST.get(),
                HOLY_RESIST_MODIFIER_ID,
                SCHOOL_RESIST_WEAKNESS,
                AttributeModifier.Operation.ADDITION
        );
    }

    @Override
    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        var modifierId = modifier.getId();
        if (MOVEMENT_SPEED_MODIFIER_UUID.equals(modifierId)) {
            return MOVEMENT_SPEED_BONUS;
        }
        if (STEP_HEIGHT_MODIFIER_UUID.equals(modifierId)) {
            return STEP_HEIGHT_ADDITION;
        }
        if (FIRE_RESIST_MODIFIER_UUID.equals(modifierId) || HOLY_RESIST_MODIFIER_UUID.equals(modifierId)) {
            return SCHOOL_RESIST_WEAKNESS;
        }

        return super.getAttributeModifierValue(amplifier, modifier);
    }
}
