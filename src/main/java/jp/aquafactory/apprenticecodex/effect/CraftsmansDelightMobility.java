package jp.aquafactory.apprenticecodex.effect;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class CraftsmansDelightMobility extends MobEffect {
    private static final double CASTING_MOVE_SPEED_BONUS = 0.8;

    public CraftsmansDelightMobility() {
        super(MobEffectCategory.BENEFICIAL, 0xD7A552);
        addAttributeModifier(
                AttributeRegistry.CASTING_MOVESPEED.get(),
                "f5d7ca80-f6ac-499a-a444-5ce9b7071db0",
                CASTING_MOVE_SPEED_BONUS,
                AttributeModifier.Operation.ADDITION
        );
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}
