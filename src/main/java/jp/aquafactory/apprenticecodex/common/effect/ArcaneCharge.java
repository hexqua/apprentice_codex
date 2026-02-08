package jp.aquafactory.apprenticecodex.common.effect;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class ArcaneCharge extends MobEffect {
    public ArcaneCharge() {
        super(MobEffectCategory.BENEFICIAL, 0x7733ff);

        // ランダムなUUIDを振ってる.
        addAttributeModifier(
                AttributeRegistry.SPELL_POWER,
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "arcane_charge_spell_power"),
                0.15,
                AttributeModifier.Operation.ADD_VALUE
        );
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false;
    }
}
