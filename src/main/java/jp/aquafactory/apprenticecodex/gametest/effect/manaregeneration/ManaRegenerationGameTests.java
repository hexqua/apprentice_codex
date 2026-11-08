package jp.aquafactory.apprenticecodex.gametest.effect.manaregeneration;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ManaRegenerationGameTests {
    private ManaRegenerationGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void manaRegenerationEffectAppliesExpectedFinalManaRegenMultiplier(GameTestHelper helper) {
        ManaRegenerationScenarios.manaRegenerationEffectAppliesExpectedFinalManaRegenMultiplier(helper);
    }
}
