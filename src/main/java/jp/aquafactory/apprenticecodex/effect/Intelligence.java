package jp.aquafactory.apprenticecodex.effect;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class Intelligence extends MobEffect {
    private static final double SPELL_POWER_BONUS_PER_LEVEL = 0.10D;
    private static final ResourceLocation SPELL_POWER_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
            "apprenticecodex",
            "intelligence_spell_power"
    );

    public Intelligence() {
        super(MobEffectCategory.BENEFICIAL, 0x1E90FF);

        addAttributeModifier(
                AttributeRegistry.SPELL_POWER,
                SPELL_POWER_MODIFIER_ID,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
                amplifier -> SPELL_POWER_BONUS_PER_LEVEL * (amplifier + 1)
        );
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false;
    }
}
