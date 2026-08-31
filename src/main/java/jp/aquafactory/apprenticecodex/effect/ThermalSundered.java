package jp.aquafactory.apprenticecodex.effect;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public final class ThermalSundered extends MobEffect {
    public static final int INITIAL_DURATION_TICKS = 60;
    public static final int ON_FIRE_EXTENDED_DURATION_TICKS = 100;
    public static final int MAX_AMPLIFIER = 4;
    private static final double FIRE_MAGIC_RESIST_REDUCTION_PER_LEVEL = -0.1D;
    private static final ResourceLocation FIRE_MAGIC_RESIST_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "thermal_sundered_fire_magic_resist");

    public ThermalSundered() {
        super(MobEffectCategory.HARMFUL, 0xFF8C00);

        addAttributeModifier(
                AttributeRegistry.FIRE_MAGIC_RESIST,
                FIRE_MAGIC_RESIST_MODIFIER_ID,
                AttributeModifier.Operation.ADD_VALUE,
                amplifier -> FIRE_MAGIC_RESIST_REDUCTION_PER_LEVEL * (clampAmplifier(amplifier) + 1)
        );
    }

    public static int clampAmplifier(int amplifier) {
        return Mth.clamp(amplifier, 0, MAX_AMPLIFIER);
    }

    public static int getFireMagicResistReductionPercent(int amplifier) {
        return 10 * (clampAmplifier(amplifier) + 1);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false;
    }
}
