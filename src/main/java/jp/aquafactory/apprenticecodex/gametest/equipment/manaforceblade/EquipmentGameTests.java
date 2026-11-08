package jp.aquafactory.apprenticecodex.gametest.equipment.manaforceblade;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void manaForceBladeAttunementAndUpgradeMergeForTooltip(GameTestHelper helper) {
        ManaForceBladeGameTestScenarios.manaForceBladeAttunementAndUpgradeMergeForTooltip(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaForceBladeAppliesSurgeAndAttunementAttributes(GameTestHelper helper) {
        ManaForceBladeGameTestScenarios.manaForceBladeAppliesSurgeAndAttunementAttributes(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaForceBladeAttackManaCostIsOncePerTick(GameTestHelper helper) {
        ManaForceBladeGameTestScenarios.manaForceBladeAttackManaCostIsOncePerTick(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaForceBladeConfigScalesDamageAndManaFormulas(GameTestHelper helper) {
        ManaForceBladeGameTestScenarios.manaForceBladeConfigScalesDamageAndManaFormulas(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaForceBladeReleaseCooldownUsesServerConfig(GameTestHelper helper) {
        ManaForceBladeGameTestScenarios.manaForceBladeReleaseCooldownUsesServerConfig(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaForceBladePerfectGuardReleaseCooldownGraceIsSingleUse(GameTestHelper helper) {
        ManaForceBladeGameTestScenarios.manaForceBladePerfectGuardReleaseCooldownGraceIsSingleUse(helper);
    }
}
