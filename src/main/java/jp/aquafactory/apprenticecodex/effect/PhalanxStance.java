package jp.aquafactory.apprenticecodex.effect;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class PhalanxStance extends DynamicCastingMobilityEffect {
    public static final int FIXED_AMPLIFIER = 0;
    public static final int MOVE_SPEED_ENABLED_AMPLIFIER = 1;
    private static final String CASTING_MOVE_SPEED_MODIFIER_ID = "e4bf23fe-3d22-4f7e-a7dc-34b89f989f2f";

    public PhalanxStance() {
        super(0x6CA9FF, CASTING_MOVE_SPEED_MODIFIER_ID);
    }

    @Override
    protected boolean isCastingMoveSpeedContributionEnabled(int amplifier) {
        return amplifier >= MOVE_SPEED_ENABLED_AMPLIFIER;
    }

    @Override
    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        return super.getAttributeModifierValue(amplifier, modifier);
    }
}
