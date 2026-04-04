package jp.aquafactory.apprenticecodex.effect;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class CastingMobility extends MobEffect {
    private static final double CASTING_MOVE_SPEED_BONUS = 0.8;

    public CastingMobility() {
        super(MobEffectCategory.BENEFICIAL, 0xD9C27A);
        addAttributeModifier(
                AttributeRegistry.CASTING_MOVESPEED.get(),
                "54ab28bc-55f3-4ea7-91fe-cd2263f76e38",
                CASTING_MOVE_SPEED_BONUS,
                AttributeModifier.Operation.ADDITION
        );
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}
