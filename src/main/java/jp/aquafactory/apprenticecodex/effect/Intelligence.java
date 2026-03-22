package jp.aquafactory.apprenticecodex.effect;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class Intelligence extends MobEffect {
    private static final double SPELL_POWER_BONUS_PER_LEVEL = 0.10D;

    public Intelligence() {
        super(MobEffectCategory.BENEFICIAL, 0x1E90FF);

        addAttributeModifier(
                AttributeRegistry.SPELL_POWER.get(),
                "352e1f58-4c1a-4c8a-8fe0-f5ec0f31995d",
                SPELL_POWER_BONUS_PER_LEVEL,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }

    @Override
    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        return modifier.getAmount() * (amplifier + 1);
    }
}
