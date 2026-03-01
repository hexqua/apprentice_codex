package jp.aquafactory.apprenticecodex.effect;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class PhalanxStance extends MobEffect {
    public static final int FIXED_AMPLIFIER = 0;
    private static final double CASTING_MOVE_SPEED_BONUS = 0.8;

    public PhalanxStance() {
        super(MobEffectCategory.BENEFICIAL, 0x6CA9FF);
        addAttributeModifier(
                AttributeRegistry.CASTING_MOVESPEED,
                ResourceLocation.fromNamespaceAndPath("apprenticecodex", "phalanx_stance_casting_movespeed"),
                CASTING_MOVE_SPEED_BONUS,
                AttributeModifier.Operation.ADD_VALUE
        );
    }
}
