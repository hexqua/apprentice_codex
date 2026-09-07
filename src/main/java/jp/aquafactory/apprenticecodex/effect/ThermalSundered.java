package jp.aquafactory.apprenticecodex.effect;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public final class ThermalSundered extends MobEffect {
    public static final int INITIAL_DURATION_TICKS = 60;
    public static final int ON_FIRE_EXTENDED_DURATION_TICKS = 100;
    public static final int MAX_AMPLIFIER = 4;
    private static final double FIRE_MAGIC_RESIST_REDUCTION_PER_LEVEL = -0.1D;
    // 1.20.1 の属性補正は ResourceLocation ではなく固定 UUID で識別する。
    private static final String FIRE_MAGIC_RESIST_MODIFIER_ID = "b12dd3ce-f6f9-4c12-995b-041cfcc4d746";

    public ThermalSundered() {
        super(MobEffectCategory.HARMFUL, 0xFF8C00);

        addAttributeModifier(
                AttributeRegistry.FIRE_MAGIC_RESIST.get(),
                FIRE_MAGIC_RESIST_MODIFIER_ID,
                FIRE_MAGIC_RESIST_REDUCTION_PER_LEVEL,
                AttributeModifier.Operation.ADDITION
        );
    }

    public static int clampAmplifier(int amplifier) {
        return Mth.clamp(amplifier, 0, MAX_AMPLIFIER);
    }

    public static int getFireMagicResistReductionPercent(int amplifier) {
        return 10 * (clampAmplifier(amplifier) + 1);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }

    @Override
    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        return FIRE_MAGIC_RESIST_REDUCTION_PER_LEVEL * (clampAmplifier(amplifier) + 1);
    }
}
