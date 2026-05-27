package jp.aquafactory.apprenticecodex.effect;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.effect.MagicMobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class ArcaneCharge extends MagicMobEffect {
    public ArcaneCharge() {
        super(MobEffectCategory.BENEFICIAL, 0x7733ff);

        // ランダムなUUIDを振ってる.
        addAttributeModifier(
                AttributeRegistry.SPELL_POWER.get(),
                "a53fcd32-215d-9baf-5e80-6da3971e6f1a",
                0.15,
                AttributeModifier.Operation.ADDITION
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
