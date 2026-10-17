package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.ironsartifice.IronsArtificeCompat;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class MagiAgentSuitArtificeGameTests {
    private MagiAgentSuitArtificeGameTests() {}

    // optional型を使うシナリオは、導入確認後にだけ読み込む。
    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.magi_agent_artifice")
    public static void hoodComposesWithModifiers(GameTestHelper helper) {
        if (!IronsArtificeCompat.isAvailable()) { helper.succeed(); return; }
        MagiAgentSuitArtificeGameTestScenarios.hoodComposesWithModifiers(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.magi_agent_artifice")
    public static void nativeRefundPreservesMagazine(GameTestHelper helper) {
        if (!IronsArtificeCompat.isAvailable()) { helper.succeed(); return; }
        MagiAgentSuitArtificeGameTestScenarios.nativeRefundPreservesMagazine(helper);
    }
}
