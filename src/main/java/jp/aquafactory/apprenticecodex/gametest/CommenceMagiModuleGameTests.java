package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.ironsartifice.IronsArtificeCompat;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CommenceMagiModuleGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final String BATCH = "apprenticecodex.commence_magi_module";

    private CommenceMagiModuleGameTests() {
    }

    // 登録時にoptional型を解決しないよう、外部MOD依存のシナリオは導入確認後に読み込む。
    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void headshotStacksAfterStandardDamage(GameTestHelper helper) throws Exception {
        if (!IronsArtificeCompat.isAvailable()) { helper.succeed(); return; }
        CommenceMagiModuleGameTestScenarios.headshotStacksAfterStandardDamage(helper);
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void piercingAndPelletsKeepIndependentHits(GameTestHelper helper) throws Exception {
        if (!IronsArtificeCompat.isAvailable()) { helper.succeed(); return; }
        CommenceMagiModuleGameTestScenarios.piercingAndPelletsKeepIndependentHits(helper);
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void insufficientManaCancelsBeforeSideEffects(GameTestHelper helper) throws Exception {
        if (!IronsArtificeCompat.isAvailable()) { helper.succeed(); return; }
        CommenceMagiModuleGameTestScenarios.insufficientManaCancelsBeforeSideEffects(helper);
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void manaChargedOnceAndFiredProfileSurvivesChanges(GameTestHelper helper) throws Exception {
        if (!IronsArtificeCompat.isAvailable()) { helper.succeed(); return; }
        CommenceMagiModuleGameTestScenarios.manaChargedOnceAndFiredProfileSurvivesChanges(helper);
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void freeShotsAndExemptShooters(GameTestHelper helper) throws Exception {
        if (!IronsArtificeCompat.isAvailable()) { helper.succeed(); return; }
        CommenceMagiModuleGameTestScenarios.freeShotsAndExemptShooters(helper);
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void canceledShotAndImpactHaveNoEffect(GameTestHelper helper) throws Exception {
        if (!IronsArtificeCompat.isAvailable()) { helper.succeed(); return; }
        CommenceMagiModuleGameTestScenarios.canceledShotAndImpactHaveNoEffect(helper);
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void tooltipAndConfigSyncPreserveValues(GameTestHelper helper) {
        if (!IronsArtificeCompat.isAvailable()) { helper.succeed(); return; }
        CommenceMagiModuleGameTestScenarios.tooltipAndConfigSyncPreserveValues(helper);
    }
}
