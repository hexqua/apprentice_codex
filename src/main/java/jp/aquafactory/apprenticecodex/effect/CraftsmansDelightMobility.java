package jp.aquafactory.apprenticecodex.effect;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class CraftsmansDelightMobility extends MobEffect {
    private static final double CASTING_MOVE_SPEED_BONUS = 0.8;
    private static final ResourceLocation CASTING_MOVE_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "craftsmans_delight_casting_movespeed");

    public CraftsmansDelightMobility() {
        super(MobEffectCategory.BENEFICIAL, 0xD7A552);
        addAttributeModifier(
                AttributeRegistry.CASTING_MOVESPEED,
                CASTING_MOVE_SPEED_MODIFIER_ID,
                CASTING_MOVE_SPEED_BONUS,
                AttributeModifier.Operation.ADD_VALUE
        );
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false;
    }
}
