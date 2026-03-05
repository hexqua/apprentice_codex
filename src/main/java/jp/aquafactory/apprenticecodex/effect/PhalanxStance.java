package jp.aquafactory.apprenticecodex.effect;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import it.unimi.dsi.fastutil.ints.Int2DoubleFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class PhalanxStance extends MobEffect {
    public static final int FIXED_AMPLIFIER = 0;
    public static final int MOVE_SPEED_ENABLED_AMPLIFIER = 1;
    private static final double CASTING_MOVE_SPEED_BONUS = 0.8;

    public PhalanxStance() {
        super(MobEffectCategory.BENEFICIAL, 0x6CA9FF);
        addAttributeModifier(
                AttributeRegistry.CASTING_MOVESPEED,
                ResourceLocation.fromNamespaceAndPath("apprenticecodex", "phalanx_stance_casting_movespeed"),
                AttributeModifier.Operation.ADD_VALUE,
                (Int2DoubleFunction) amplifier -> amplifier >= MOVE_SPEED_ENABLED_AMPLIFIER
                        ? CASTING_MOVE_SPEED_BONUS
                        : 0.0d
        );
    }
}
