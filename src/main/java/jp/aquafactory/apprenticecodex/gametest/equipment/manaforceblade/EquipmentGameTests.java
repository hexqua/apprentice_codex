package jp.aquafactory.apprenticecodex.gametest.equipment.manaforceblade;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void manaForceBladeAttunementAndUpgradeMergeForTooltip(GameTestHelper helper) {
        ManaForceBladeGameTestScenarios.manaForceBladeAttunementAndUpgradeMergeForTooltip(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void manaForceBladeAppliesSurgeAndAttunementAttributes(GameTestHelper helper) {
        ManaForceBladeGameTestScenarios.manaForceBladeAppliesSurgeAndAttunementAttributes(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void manaForceBladeSharpnessTooltipDamageScalesWithImbue(GameTestHelper helper) {
        ManaForceBladeGameTestScenarios.manaForceBladeSharpnessTooltipDamageScalesWithImbue(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void manaForceBladeAttackManaCostIsOncePerTick(GameTestHelper helper) {
        ManaForceBladeGameTestScenarios.manaForceBladeAttackManaCostIsOncePerTick(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void manaForceBladeConfigScalesDamageAndManaFormulas(GameTestHelper helper) {
        ManaForceBladeGameTestScenarios.manaForceBladeConfigScalesDamageAndManaFormulas(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void manaForceBladeReleaseCooldownUsesServerConfig(GameTestHelper helper) {
        ManaForceBladeGameTestScenarios.manaForceBladeReleaseCooldownUsesServerConfig(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void manaForceBladePerfectGuardReleaseCooldownGraceIsSingleUse(GameTestHelper helper) {
        ManaForceBladeGameTestScenarios.manaForceBladePerfectGuardReleaseCooldownGraceIsSingleUse(helper);
    }
}
