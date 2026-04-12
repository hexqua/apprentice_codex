package jp.aquafactory.apprenticecodex.effect;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class ManaRegeneration extends MobEffect {
    private static final double MANA_REGEN_BONUS_PER_LEVEL = 0.25D;

    public ManaRegeneration() {
        super(MobEffectCategory.BENEFICIAL, 0x8899FF);

        addAttributeModifier(
                AttributeRegistry.MANA_REGEN.get(),
                "e6c64b72-5543-4d66-a5f8-87c925ad91cb",
                MANA_REGEN_BONUS_PER_LEVEL,
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
