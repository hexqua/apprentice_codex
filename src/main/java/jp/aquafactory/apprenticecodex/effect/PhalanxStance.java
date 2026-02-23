package jp.aquafactory.apprenticecodex.effect;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class PhalanxStance extends MobEffect {
    public static final int FIXED_AMPLIFIER = 0;
    private static final double CASTING_MOVE_SPEED_BONUS = 0.8;

    public PhalanxStance() {
        super(MobEffectCategory.BENEFICIAL, 0x6CA9FF);
        addAttributeModifier(
                AttributeRegistry.CASTING_MOVESPEED.get(),
                "e4bf23fe-3d22-4f7e-a7dc-34b89f989f2f",
                CASTING_MOVE_SPEED_BONUS,
                AttributeModifier.Operation.ADDITION
        );
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }

    @Override
    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        return modifier.getAmount();
    }
}
