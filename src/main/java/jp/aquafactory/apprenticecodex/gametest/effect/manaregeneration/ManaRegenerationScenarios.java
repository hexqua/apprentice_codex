package jp.aquafactory.apprenticecodex.gametest.effect.manaregeneration;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.effect.ManaRegeneration;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

final class ManaRegenerationScenarios {
    private ManaRegenerationScenarios() {}

    static void manaRegenerationEffectAppliesExpectedFinalManaRegenMultiplier(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var effect = (ManaRegeneration) EffectRegistry.MANA_REGENERATION.get();
            var manaRegenModifier = effect.getAttributeModifiers().get(AttributeRegistry.MANA_REGEN.get());
            helper.assertTrue(manaRegenModifier != null, "Mana Regeneration is missing the mana regen attribute modifier");
            helper.assertTrue(manaRegenModifier != null
                            && manaRegenModifier.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL,
                    "Mana Regeneration should use MULTIPLY_TOTAL but got "
                            + (manaRegenModifier == null ? "missing" : manaRegenModifier.getOperation()));

            var levelOneAmount = manaRegenModifier == null ? Double.NaN : effect.getAttributeModifierValue(0, manaRegenModifier);
            helper.assertTrue(Math.abs(levelOneAmount - 0.25D) < 1.0e-9D,
                    "Mana Regeneration Lv1 regression: expected 0.25 but got " + levelOneAmount);

            var levelTwoAmount = manaRegenModifier == null ? Double.NaN : effect.getAttributeModifierValue(1, manaRegenModifier);
            helper.assertTrue(Math.abs(levelTwoAmount - 0.50D) < 1.0e-9D,
                    "Mana Regeneration Lv2 regression: expected 0.50 but got " + levelTwoAmount);
        });
    }
}
