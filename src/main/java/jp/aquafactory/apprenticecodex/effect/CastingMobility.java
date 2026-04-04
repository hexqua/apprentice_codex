package jp.aquafactory.apprenticecodex.effect;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class CastingMobility extends MobEffect {
    private static final double CASTING_MOVE_SPEED_BONUS = 0.8;

    public CastingMobility() {
        super(MobEffectCategory.BENEFICIAL, 0xD9C27A);
        addAttributeModifier(
                AttributeRegistry.CASTING_MOVESPEED,
                ResourceLocation.fromNamespaceAndPath("apprenticecodex", "casting_mobility_move_speed"),
                CASTING_MOVE_SPEED_BONUS,
                AttributeModifier.Operation.ADD_VALUE
        );
    }
}
